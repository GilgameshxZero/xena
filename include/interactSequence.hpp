#pragma once

#include <rain.hpp>

namespace Xena {
	// Tracks the state of a single touch ID through its various events.
	class InteractSequence {
		public:
		static inline std::chrono::steady_clock::duration const
			IGNORE_SHORT_THRESHOLD{std::chrono::milliseconds(100)};
		static inline LONG const CONTACT_SIZE_THRESHOLD{2};

		enum State { DOWN, UP, MOVE };

		DWORD id;
		bool isPen;
		std::size_t cSequence;

		POINT position;
		LONG contactSizeMax{0};
		std::chrono::time_point<std::chrono::steady_clock> timeDown;
	};
}
