#pragma once

#include <interaction.hpp>
#include <painter.hpp>

#include <rain.hpp>

namespace Xena {
	class Touch {
		private:
		Painter &painter;

		public:
		Touch(Painter &);

		void onTouchDown(
			Interaction &,
			std::chrono::steady_clock::time_point,
			POINT);
		void
		onTouchUp(Interaction &, std::chrono::steady_clock::time_point, POINT);
		void onTouchMove(
			Interaction &,
			std::chrono::steady_clock::time_point,
			POINT);
	};
}
