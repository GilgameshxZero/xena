#include <main-window.hpp>
#include <pen.hpp>
#include <touch.hpp>

namespace Xena {
	MainWindow::MainWindow(std::string const &fileToLoad)
			: Window({.dwStyle = WS_POPUP | WS_VISIBLE | WS_MAXIMIZE}),
				painter(fileToLoad, *this),
				mouse(this->painter),
				touch(this->painter),
				pen(this->painter),
				eraser(this->painter) {}

	LRESULT MainWindow::onCreate(WPARAM wParam, LPARAM lParam) {
		Rain::Windows::validateSystemCall(RegisterTouchWindow(*this, NULL));
		return 0;
	}
	LRESULT MainWindow::onDestroy(WPARAM wParam, LPARAM lParam) {
		this->painter.svg.save();
		PostQuitMessage(0);
		return 0;
	}
	LRESULT MainWindow::onPaint(WPARAM wParam, LPARAM lParam) {
		return this->painter.onPaint(wParam, lParam);
	}
	LRESULT MainWindow::onPointerDown(WPARAM wParam, LPARAM lParam) {
		return this->onPointerEvent(wParam, lParam);
	}
	LRESULT MainWindow::onPointerUp(WPARAM wParam, LPARAM lParam) {
		return this->onPointerEvent(wParam, lParam);
	}
	LRESULT MainWindow::onPointerUpdate(WPARAM wParam, LPARAM lParam) {
		return this->onPointerEvent(wParam, lParam);
	}

	LRESULT MainWindow::onPointerEvent(WPARAM wParam, LPARAM lParam) {
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
							interaction, now, pointerInfo->ptPixelLocation);
						break;
					case Interaction::Type::TOUCH:
						this->touch.onTouchDown(
							interaction, now, pointerInfo->ptPixelLocation);
						break;
					case Interaction::Type::PEN:
						this->pen.onPenDown(
							interaction, now, pointerInfo->ptHimetricLocation);
						break;
					case Interaction::Type::ERASER:
						this->eraser.onEraserDown(
							interaction, now, pointerInfo->ptPixelLocation);
						break;
				}
				break;
			case Interaction::State::UP:
				switch (interaction.type) {
					case Interaction::Type::MOUSE:
						this->mouse.onMouseUp(
							interaction, now, pointerInfo->ptPixelLocation);
						break;
					case Interaction::Type::TOUCH:
						this->touch.onTouchUp(
							interaction, now, pointerInfo->ptPixelLocation);
						break;
					case Interaction::Type::PEN:
						this->pen.onPenUp(
							interaction, now, pointerInfo->ptHimetricLocation);
						break;
					case Interaction::Type::ERASER:
						this->eraser.onEraserUp(
							interaction, now, pointerInfo->ptPixelLocation);
						break;
				}
				this->interactions.erase(j);
				break;
			case Interaction::State::MOVE:
				switch (interaction.type) {
					case Interaction::Type::MOUSE:
						this->mouse.onMouseMove(
							interaction, now, pointerInfo->ptPixelLocation);
						break;
					case Interaction::Type::TOUCH:
						this->touch.onTouchMove(
							interaction, now, pointerInfo->ptPixelLocation);
						break;
					case Interaction::Type::PEN:
						this->pen.onPenMove(
							interaction, now, pointerInfo->ptHimetricLocation);
						break;
					case Interaction::Type::ERASER:
						this->eraser.onEraserMove(
							interaction, now, pointerInfo->ptPixelLocation);
						break;
				}
				break;
		}
		interaction.state = state;
		interaction.position = pointerInfo->ptPixelLocation;
		return 0;
	}
}
