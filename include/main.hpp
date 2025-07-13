#pragma once

#include <interactSequence.hpp>

#include <rain.hpp>

namespace Xena {
	class Main {
		public:
		static inline HBRUSH brush{(HBRUSH)COLOR_WINDOW};

		static void create();

		private:
		static inline std::unordered_map<DWORD, InteractSequence> interactSequences;

		static LRESULT CALLBACK wndProc(HWND, UINT, WPARAM, LPARAM);

		static LRESULT onDestroy(HWND, WPARAM, LPARAM);
		static LRESULT onPointer(HWND, WPARAM, LPARAM);
	};
}
