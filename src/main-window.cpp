#include <main-window.hpp>
#include <pen.hpp>
#include <touch.hpp>

namespace Xena {
	MainWindow::MainWindow(std::string const &fileToLoad)
			: painter(fileToLoad, this->createWindow()),
				mouse(this->painter),
				touch(this->painter),
				pen(this->painter),
				eraser(this->painter) {
		this->painter.setTheme(Rain::Windows::isLightTheme());
		this->painter.refreshDpi();
	}

	LRESULT CALLBACK
	MainWindow::wndProc(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {
		return Rain::Error::consumeThrowable(
			[&]() {
				MainWindow *that{reinterpret_cast<MainWindow *>(
					GetWindowLongPtr(hWnd, GWLP_USERDATA))};
				if (that == NULL) {
					return DefWindowProc(hWnd, uMsg, wParam, lParam);
				}
				switch (uMsg) {
					case WM_DESTROY:
						return that->onDestroy(hWnd, wParam, lParam);
					case WM_PAINT:
						return that->onPaint(hWnd, wParam, lParam);
					case WM_POINTERDOWN:
						return that->onPointerDown(hWnd, wParam, lParam);
					case WM_POINTERUP:
						return that->onPointerUp(hWnd, wParam, lParam);
					case WM_POINTERUPDATE:
						return that->onPointerUpdate(hWnd, wParam, lParam);
					case WM_SETTINGCHANGE:
						return that->onSettingsChange(hWnd, wParam, lParam);
					default:
						return DefWindowProc(hWnd, uMsg, wParam, lParam);
				}
			},
			"MainWindow::wndProc")();
	}

	HWND MainWindow::createWindow() {
		HINSTANCE hInstance{GetModuleHandle(NULL)};
		WNDCLASSEX wndClassEx{
			sizeof(WNDCLASSEX),
			CS_HREDRAW | CS_VREDRAW,
			MainWindow::wndProc,
			0,
			0,
			hInstance,
			NULL,
			LoadCursor(NULL, IDC_ARROW),
			(HBRUSH)(COLOR_WINDOW + 1),
			NULL,
			typeid(*this).name(),
			NULL};
		Rain::Windows::validateSystemCall(RegisterClassEx(&wndClassEx));
		HWND hWnd{Rain::Windows::validateSystemCall(CreateWindowEx(
			NULL,
			wndClassEx.lpszClassName,
			"",
			WS_POPUP | WS_VISIBLE | WS_MAXIMIZE,
			0,
			0,
			0,
			0,
			NULL,
			NULL,
			hInstance,
			NULL))};
		SetWindowLongPtr(hWnd, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(this));
		Rain::Windows::validateSystemCall(RegisterTouchWindow(hWnd, NULL));
		Rain::Log::verbose("MainWindow::MainWindow: Created window.");
		return hWnd;
	}

	LRESULT MainWindow::onDestroy(HWND hWnd, WPARAM wParam, LPARAM lParam) {
		this->painter.svg.save();
		PostQuitMessage(0);
		return 0;
	}
	LRESULT MainWindow::onPaint(HWND hWnd, WPARAM wParam, LPARAM lParam) {
		return this->painter.onPaint(hWnd, wParam, lParam);
	}
	LRESULT MainWindow::onPointerDown(HWND hWnd, WPARAM wParam, LPARAM lParam) {
		return this->onPointerEvent(hWnd, wParam, lParam);
	}
	LRESULT MainWindow::onPointerUp(HWND hWnd, WPARAM wParam, LPARAM lParam) {
		return this->onPointerEvent(hWnd, wParam, lParam);
	}
	LRESULT MainWindow::onPointerUpdate(HWND hWnd, WPARAM wParam, LPARAM lParam) {
		return this->onPointerEvent(hWnd, wParam, lParam);
	}
	LRESULT
	MainWindow::onSettingsChange(HWND hWnd, WPARAM wParam, LPARAM lParam) {
		this->painter.setTheme(Rain::Windows::isLightTheme());
		this->painter.refreshDpi();
		this->painter.rePaint();
		return 0;
	}

	LRESULT MainWindow::onPointerEvent(HWND hWnd, WPARAM wParam, LPARAM lParam) {
		POINTER_INPUT_TYPE pointerInputType;
		Rain::Windows::validateSystemCall(
			GetPointerType(GET_POINTERID_WPARAM(wParam), &pointerInputType));
		bool isPen{pointerInputType == PT_PEN},
			isTouch{pointerInputType == PT_TOUCH}, isEraser{false};
		LONG contactSize{0};

		std::unique_ptr<POINTER_INFO> pointerInfoManaged;
		POINTER_INFO *pointerInfo;
		POINTER_PEN_INFO pointerPenInfo;
		POINTER_TOUCH_INFO pointerTouchInfo;
		if (isPen) {
			Rain::Windows::validateSystemCall(
				GetPointerPenInfo(GET_POINTERID_WPARAM(wParam), &pointerPenInfo));
			pointerInfo = &pointerPenInfo.pointerInfo;
			isEraser = (pointerPenInfo.penFlags & PEN_FLAG_ERASER) > 0;
		} else if (isTouch) {
			Rain::Windows::validateSystemCall(
				GetPointerTouchInfo(GET_POINTERID_WPARAM(wParam), &pointerTouchInfo));
			pointerInfo = &pointerTouchInfo.pointerInfo;
			if ((pointerTouchInfo.touchMask & TOUCH_MASK_CONTACTAREA) > 0) {
				contactSize =
					(pointerTouchInfo.rcContact.right - pointerTouchInfo.rcContact.left) *
					(pointerTouchInfo.rcContact.bottom - pointerTouchInfo.rcContact.top);
			}
		} else {
			pointerInfoManaged.reset(new POINTER_INFO);
			pointerInfo = pointerInfoManaged.get();
			Rain::Windows::validateSystemCall(
				GetPointerInfo(GET_POINTERID_WPARAM(wParam), pointerInfo));
		}
		bool isContact{(pointerInfo->pointerFlags & POINTER_FLAG_INCONTACT) > 0},
			isDown{(pointerInfo->pointerFlags & POINTER_FLAG_DOWN) > 0},
			isUp{(pointerInfo->pointerFlags & POINTER_FLAG_UP) > 0},
			isMove{(pointerInfo->pointerFlags & POINTER_FLAG_UPDATE) > 0};
		if (!isContact && !isUp) {
			return 0;
		}

		// Build the interaction.
		Interaction::Type type{
			isPen ? (isEraser ? Interaction::Type::ERASER : Interaction::Type::PEN)
						: (isTouch ? Interaction::Type::TOUCH : Interaction::Type::MOUSE)};
		Interaction::State state{
			isDown ? Interaction::State::DOWN
						 : (isUp ? Interaction::State::UP : Interaction::State::MOVE)};
		auto j{this->interactions.find(pointerInfo->pointerId)};
		if (j == this->interactions.end()) {
			if (state != Interaction::State::DOWN) {
				return 0;
			}
			j = this->interactions
						.emplace(
							pointerInfo->pointerId,
							Interaction{
								pointerInfo->pointerId, type, state, this->interactions.size()})
						.first;
		}
		Interaction &interaction{j->second};

		std::chrono::steady_clock::time_point now{std::chrono::steady_clock::now()};
		interaction.contactSizeMax = max(interaction.contactSizeMax, contactSize);
		// An interaction is not allowed to change type in its lifetime.
		switch (state) {
			case Interaction::State::DOWN:
				interaction.timeDown = now;
				switch (interaction.type) {
					case Interaction::Type::MOUSE:
						this->mouse.onMouseDown(
							interaction, now, pointerInfo->ptHimetricLocation);
						break;
					case Interaction::Type::TOUCH:
						this->touch.onTouchDown(
							interaction, now, pointerInfo->ptHimetricLocation);
						break;
					case Interaction::Type::PEN:
						this->pen.onPenDown(
							interaction, now, pointerInfo->ptHimetricLocation);
						break;
					case Interaction::Type::ERASER:
						this->eraser.onEraserDown(
							interaction, now, pointerInfo->ptHimetricLocation);
						break;
				}
				break;
			case Interaction::State::UP:
				switch (interaction.type) {
					case Interaction::Type::MOUSE:
						this->mouse.onMouseUp(
							interaction, now, pointerInfo->ptHimetricLocation);
						break;
					case Interaction::Type::TOUCH:
						this->touch.onTouchUp(
							interaction, now, pointerInfo->ptHimetricLocation);
						break;
					case Interaction::Type::PEN:
						this->pen.onPenUp(
							interaction, now, pointerInfo->ptHimetricLocation);
						break;
					case Interaction::Type::ERASER:
						this->eraser.onEraserUp(
							interaction, now, pointerInfo->ptHimetricLocation);
						break;
				}
				this->interactions.erase(j);
				break;
			case Interaction::State::MOVE:
				switch (interaction.type) {
					case Interaction::Type::MOUSE:
						this->mouse.onMouseMove(
							interaction, now, pointerInfo->ptHimetricLocation);
						break;
					case Interaction::Type::TOUCH:
						this->touch.onTouchMove(
							interaction, now, pointerInfo->ptHimetricLocation);
						break;
					case Interaction::Type::PEN:
						this->pen.onPenMove(
							interaction, now, pointerInfo->ptHimetricLocation);
						break;
					case Interaction::Type::ERASER:
						this->eraser.onEraserMove(
							interaction, now, pointerInfo->ptHimetricLocation);
						break;
				}
				break;
		}
		interaction.state = state;
		interaction.position = pointerInfo->ptHimetricLocation;
		return 0;
	}
}
