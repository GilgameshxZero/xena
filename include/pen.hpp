#pragma once

#include <interaction.hpp>
#include <painter.hpp>

#include <rain.hpp>

namespace Xena {
	class Pen {
		private:
		long double const HIMETRIC_TO_PX;

		Painter &painter;

		std::shared_ptr<Path> path;

		public:
		bool isDrawing{false};

		Pen(Painter &);

		void onPenDown(Interaction &, std::chrono::steady_clock::time_point, POINT);
		void onPenUp(Interaction &, std::chrono::steady_clock::time_point, POINT);
		void onPenMove(Interaction &, std::chrono::steady_clock::time_point, POINT);
	};
}
