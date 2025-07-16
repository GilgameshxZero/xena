#pragma once

#include <interaction.hpp>
#include <painter.hpp>

#include <rain.hpp>

namespace Xena {
	class Eraser {
		private:
		Painter &painter;

		public:
		Eraser(Painter &);

		void
		onEraserDown(Interaction &, std::chrono::steady_clock::time_point, POINT);
		void
		onEraserUp(Interaction &, std::chrono::steady_clock::time_point, POINT);
		void
		onEraserMove(Interaction &, std::chrono::steady_clock::time_point, POINT);
	};
}
