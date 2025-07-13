#include <main.hpp>

namespace Xena {
	Main::Main() {
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
			WS_POPUP | WS_VISIBLE,	// | WS_MAXIMIZE,
			0,
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
			case WM_TOUCH:
				return Main::onTouch(hWnd, wParam, lParam);
			case WM_DESTROY:
				return Main::onDestroy(hWnd, wParam, lParam);
			default:
				return DefWindowProc(hWnd, uMsg, wParam, lParam);
		}
	}

	LRESULT Main::onDestroy(HWND hWnd, WPARAM wParam, LPARAM lParam) {
		PostQuitMessage(0);
		return 0;
	}

	LRESULT Main::onTouch(HWND hWnd, WPARAM wParam, LPARAM lParam) {
		WORD cTouchPoints{LOWORD(wParam)};
		std::vector<TOUCHINPUT> touchInputs(cTouchPoints);
		Rain::Windows::validateSystemCall(GetTouchInputInfo(
			reinterpret_cast<HTOUCHINPUT>(lParam),
			cTouchPoints,
			touchInputs.data(),
			sizeof(TOUCHINPUT)));

		for (auto &i : touchInputs) {
			DWORD contactSizeMax{0};
			if ((i.dwMask & TOUCHINPUTMASKF_CONTACTAREA) > 0) {
				contactSizeMax = max(i.cxContact, i.cyContact);
			}
			bool isPen{(i.dwFlags & TOUCHEVENTF_PEN) > 0},
				isPalm{
					(i.dwFlags & TOUCHEVENTF_PEN) > 0 ||
					contactSizeMax > Main::PALM_SIZE_THRESHOLD},
				isDown{(i.dwFlags & TOUCHEVENTF_DOWN) > 0},
				isUp{(i.dwFlags & TOUCHEVENTF_UP) > 0},
				isMove{(i.dwFlags & TOUCHEVENTF_MOVE) > 0};

			if (isPalm && !isPen) {
				continue;
			}
			if (isDown) {
				if (isPen) {
					Rain::Log::verbose("Main::onTouch: ", i.dwID, " DOWN, PEN.");
				} else {
					Rain::Log::verbose(
						"Main::onTouch: ",
						i.dwID,
						" DOWN, TOUCH, contactSizeMax = ",
						contactSizeMax);
				}
			} else if (isUp) {
				if (isPen) {
					Rain::Log::verbose("Main::onTouch: ", i.dwID, " UP, PEN.");
				} else {
					Rain::Log::verbose(
						"Main::onTouch: ",
						i.dwID,
						" UP, TOUCH, contactSizeMax = ",
						contactSizeMax);
				}
			} else if (isMove) {
				if (isPen) {
					Rain::Log::verbose("Main::onTouch: ", i.dwID, " MOVE, PEN.");
				} else {
					Rain::Log::verbose(
						"Main::onTouch: ",
						i.dwID,
						" MOVE, TOUCH, contactSizeMax = ",
						contactSizeMax);
				}
			}
		}
		return 0;
	}
}
