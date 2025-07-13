#include <xenaProc.hpp>

namespace Xena {
	LRESULT CALLBACK xenaProc(
		_In_ HWND hWnd,
		_In_ UINT uMsg,
		_In_ WPARAM wParam,
		_In_ LPARAM lParam) {
		switch (uMsg) {
			default:
				break;
		}
		return DefWindowProc(hWnd, uMsg, wParam, lParam);
	}
}
