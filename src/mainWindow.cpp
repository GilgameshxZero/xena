#include <mainWindow.hpp>
#include <penManager.hpp>
#include <touchManager.hpp>

namespace Xena {
	MainWindow::MainWindow(std::string const &fileToLoad)
			: windowManager(fileToLoad),
				penManager(this->windowManager),
				touchManager(this->windowManager) {
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
		this->windowManager.hWnd = Rain::Windows::validateSystemCall(CreateWindowEx(
			NULL,
			wndClassEx.lpszClassName,
			"",
			WS_POPUP | WS_VISIBLE | WS_MAXIMIZE,
			500,
			0,
			500,
			500,
			NULL,
			NULL,
			hInstance,
			NULL));
		SetWindowLongPtr(
			this->windowManager.hWnd,
			GWLP_USERDATA,
			reinterpret_cast<LONG_PTR>(this));
		Rain::Windows::validateSystemCall(
			RegisterTouchWindow(this->windowManager.hWnd, NULL));
		Rain::Log::verbose("MainWindow::MainWindow: Created window.");
	}

	LRESULT CALLBACK
	MainWindow::wndProc(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {
		MainWindow *that{
			reinterpret_cast<MainWindow *>(GetWindowLongPtr(hWnd, GWLP_USERDATA))};
		if (that == NULL) {
			return DefWindowProc(hWnd, uMsg, wParam, lParam);
		}
		switch (uMsg) {
			case WM_DESTROY:
				return that->onDestroy(hWnd, wParam, lParam);
			case WM_PAINT:
				return that->onPaint(hWnd, wParam, lParam);
			case WM_POINTERDOWN:
			case WM_POINTERUP:
			case WM_POINTERUPDATE:
				return that->onPointer(hWnd, wParam, lParam);
			default:
				return DefWindowProc(hWnd, uMsg, wParam, lParam);
		}
	}

	LRESULT MainWindow::onDestroy(HWND hWnd, WPARAM wParam, LPARAM lParam) {
		PostQuitMessage(0);
		return 0;
	}

	LRESULT MainWindow::onPaint(HWND hWnd, WPARAM wParam, LPARAM lParam) {
		PAINTSTRUCT ps;
		HDC hdc = BeginPaint(hWnd, &ps);
		FillRect(hdc, &ps.rcPaint, this->windowManager.brush);
		EndPaint(hWnd, &ps);
		Rain::Log::verbose("MainWindow::onPaint.");
		return 0;
	}

	LRESULT MainWindow::onPointer(HWND hWnd, WPARAM wParam, LPARAM lParam) {
		POINTER_INPUT_TYPE pointerInputType;
		Rain::Windows::validateSystemCall(
			GetPointerType(GET_POINTERID_WPARAM(wParam), &pointerInputType));
		bool isPen{pointerInputType == PT_PEN},
			isTouch{pointerInputType == PT_TOUCH};
		if (!isPen && !isTouch) {
			return 0;
		}

		bool isEraser{false};
		POINTER_INFO *pointerInfo;
		POINTER_PEN_INFO pointerPenInfo;
		POINTER_TOUCH_INFO pointerTouchInfo;
		LONG contactSize{0};
		if (isPen) {
			Rain::Windows::validateSystemCall(
				GetPointerPenInfo(GET_POINTERID_WPARAM(wParam), &pointerPenInfo));
			pointerInfo = &pointerPenInfo.pointerInfo;
			isEraser = (pointerPenInfo.penFlags & PEN_FLAG_ERASER) > 0;
		} else {
			Rain::Windows::validateSystemCall(
				GetPointerTouchInfo(GET_POINTERID_WPARAM(wParam), &pointerTouchInfo));
			pointerInfo = &pointerTouchInfo.pointerInfo;
			if ((pointerTouchInfo.touchMask & TOUCH_MASK_CONTACTAREA) > 0) {
				contactSize =
					(pointerTouchInfo.rcContact.right - pointerTouchInfo.rcContact.left) *
					(pointerTouchInfo.rcContact.bottom - pointerTouchInfo.rcContact.top);
			}
		}
		bool isContact{(pointerInfo->pointerFlags & POINTER_FLAG_INCONTACT) > 0},
			isDown{(pointerInfo->pointerFlags & POINTER_FLAG_DOWN) > 0},
			isUp{(pointerInfo->pointerFlags & POINTER_FLAG_UP) > 0},
			isMove{(pointerInfo->pointerFlags & POINTER_FLAG_UPDATE) > 0};
		if (!isContact && !isUp) {
			return 0;
		}

		InteractSequence::State state{
			isDown
				? InteractSequence::State::DOWN
				: (isUp ? InteractSequence::State::UP : InteractSequence::State::MOVE)};
		this->interactSequences.insert(
			{pointerInfo->pointerId, InteractSequence()});
		auto j{this->interactSequences.find(pointerInfo->pointerId)};
		if (j == this->interactSequences.end()) {
			j =
				this->interactSequences
					.insert(
						{pointerInfo->pointerId,
						 InteractSequence{
							 pointerInfo->pointerId, isPen, this->interactSequences.size()}})
					.first;
			state = InteractSequence::State::DOWN;
		}
		InteractSequence &sequence{j->second};

		std::chrono::steady_clock::time_point now{std::chrono::steady_clock::now()};
		sequence.contactSizeMax = max(sequence.contactSizeMax, contactSize);
		switch (state) {
			case InteractSequence::State::DOWN:
				sequence.timeDown = now;
				if (isPen && !isEraser) {
					this->penManager.onPenDown(
						sequence, now, pointerInfo->ptHimetricLocation);
				} else if (isPen) {
					this->penManager.onEraserDown(
						sequence, now, pointerInfo->ptHimetricLocation);
				} else {
					this->touchManager.onTouchDown(
						sequence, now, pointerInfo->ptHimetricLocation);
				}
				break;
			case InteractSequence::State::UP:
				if (isPen && !isEraser) {
					this->penManager.onPenUp(
						sequence, now, pointerInfo->ptHimetricLocation);
				} else if (isPen) {
					this->penManager.onEraserUp(
						sequence, now, pointerInfo->ptHimetricLocation);
				} else {
					this->touchManager.onTouchUp(
						sequence, now, pointerInfo->ptHimetricLocation);
				}
				this->interactSequences.erase(j);
				break;
			case InteractSequence::State::MOVE:
				if (isPen && !isEraser) {
					this->penManager.onPenMove(
						sequence, now, pointerInfo->ptHimetricLocation);
				} else if (isPen) {
					this->penManager.onEraserMove(
						sequence, now, pointerInfo->ptHimetricLocation);
				} else {
					this->touchManager.onTouchMove(
						sequence, now, pointerInfo->ptHimetricLocation);
				}
				break;
		}
		sequence.position = pointerInfo->ptHimetricLocation;
		return 0;
	}
}
