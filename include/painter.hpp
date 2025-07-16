#pragma once

#include <svg.hpp>

#include <rain.hpp>

namespace Xena {
	class Painter {
		private:
		static inline float const STROKE_WIDTH_DP{2.5};
		float STROKE_WIDTH_PX{Painter::STROKE_WIDTH_DP};

		// Chunks are created with a fixed DPI which does not change throughout its
		// lifetime.
		static inline PointLl const CHUNK_SIZE_PX{512, 512};

		HWND hWnd;
		HDC hDc;

		bool isLightTheme{true};
		Gdiplus::SolidBrush blackBrush{Gdiplus::Color(0xff000000)},
			whiteBrush{Gdiplus::Color(0xffffffff)};
		Gdiplus::Pen blackPen{Gdiplus::Color(0xff000000), this->STROKE_WIDTH_PX},
			whitePen{Gdiplus::Color(0xffffffff), this->STROKE_WIDTH_PX},
			transparentPen{Gdiplus::Color(0x00000000), this->STROKE_WIDTH_PX * 1.5f};

		PointLl viewportOffset, currentChunk;

		// Maps a chunk coordinate to the chunk, and all path IDs in the chunk.
		std::unordered_map<
			PointLl,
			std::pair<std::shared_ptr<Chunk>, std::unordered_set<std::size_t>>>
			chunks;
		// Maps a path ID to a path, and all chunks the path spans.
		std::unordered_map<
			std::size_t,
			std::pair<std::shared_ptr<Path>, std::unordered_set<PointLl>>>
			paths;

		public:
		Svg svg;

		Painter(std::string const &, HWND);

		void rePaint();
		LRESULT onPaint(HWND, WPARAM, LPARAM);

		void setTheme(bool);
		void refreshDpi();

		void addPath(std::shared_ptr<Path> const &);
		void removePath(std::size_t);
	};
}
