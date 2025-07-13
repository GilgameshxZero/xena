#include <rain.hpp>

namespace Xena {
	class Main {
		public:
		Main();

		private:
		static inline DWORD PALM_SIZE_THRESHOLD{150};

		static LRESULT CALLBACK wndProc(HWND, UINT, WPARAM, LPARAM);

		static LRESULT onDestroy(HWND, WPARAM, LPARAM);
		static LRESULT onTouch(HWND, WPARAM, LPARAM);
	};
}
