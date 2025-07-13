#include <main.hpp>
#include <penManager.hpp>
#include <touchManager.hpp>

namespace Xena {
	void Main::create() {
		HINSTANCE hInstance{GetModuleHandle(NULL)};
		WNDCLASSEX wndClassEx{
			sizeof(WNDCLASSEX),
			CS_HREDRAW | CS_VREDRAW,
			Xena::Main::wndProc,
			0,
			0,
			hInstance,
			NULL,
			LoadCursor(NULL, IDC_ARROW),
			(HBRUSH)(COLOR_WINDOW + 1),
			NULL,
			"main",
			NULL};
		Rain::Windows::validateSystemCall(RegisterClassEx(&wndClassEx));
		HWND hWnd{Rain::Windows::validateSystemCall(CreateWindowEx(
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
			NULL))};
		Rain::Windows::validateSystemCall(RegisterTouchWindow(hWnd, NULL));
		Rain::Log::verbose("Main::Main: Created window.");
	}

	LRESULT CALLBACK
	Main::wndProc(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {
		switch (uMsg) {
			case WM_DESTROY:
				return Main::onDestroy(hWnd, wParam, lParam);
			case WM_POINTERDOWN:
			case WM_POINTERUP:
			case WM_POINTERUPDATE:
				return Main::onPointer(hWnd, wParam, lParam);
			default:
				return DefWindowProc(hWnd, uMsg, wParam, lParam);
		}
	}

	LRESULT Main::onDestroy(HWND hWnd, WPARAM wParam, LPARAM lParam) {
		PostQuitMessage(0);
		return 0;
	}

	LRESULT Main::onPointer(HWND hWnd, WPARAM wParam, LPARAM lParam) {
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
		auto j{Main::interactSequences.find(pointerInfo->pointerId)};
		if (j == Main::interactSequences.end()) {
			j =
				Main::interactSequences
					.insert(
						{pointerInfo->pointerId,
						 InteractSequence{
							 pointerInfo->pointerId, isPen, Main::interactSequences.size()}})
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
					PenManager::onPenDown(sequence, now, pointerInfo->ptHimetricLocation);
				} else if (isPen) {
					PenManager::onEraserDown(
						sequence, now, pointerInfo->ptHimetricLocation);
				} else {
					TouchManager::onTouchDown(
						sequence, now, pointerInfo->ptHimetricLocation);
				}
				break;
			case InteractSequence::State::UP:
				if (isPen && !isEraser) {
					PenManager::onPenUp(sequence, now, pointerInfo->ptHimetricLocation);
				} else if (isPen) {
					PenManager::onEraserUp(
						sequence, now, pointerInfo->ptHimetricLocation);
				} else {
					TouchManager::onTouchUp(
						sequence, now, pointerInfo->ptHimetricLocation);
				}
				Main::interactSequences.erase(j);
				break;
			case InteractSequence::State::MOVE:
				if (isPen && !isEraser) {
					PenManager::onPenMove(sequence, now, pointerInfo->ptHimetricLocation);
				} else if (isPen) {
					PenManager::onEraserMove(
						sequence, now, pointerInfo->ptHimetricLocation);
				} else {
					TouchManager::onTouchMove(
						sequence, now, pointerInfo->ptHimetricLocation);
				}
				break;
		}
		sequence.position = pointerInfo->ptHimetricLocation;
		return 0;
	}
}
