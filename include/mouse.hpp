#pragma once

#include <interaction.hpp>
#include <painter.hpp>

#include <rain.hpp>

namespace Xena {
	class Mouse {
		private:
		Painter &painter;

		public:
		Mouse(Painter &);

		void
		onMouseDown(Interaction &, std::chrono::steady_clock::time_point, POINT);
		void
		onMouseUp(Interaction &, std::chrono::steady_clock::time_point, POINT);
		void
		onMouseMove(Interaction &, std::chrono::steady_clock::time_point, POINT);
	};
}
