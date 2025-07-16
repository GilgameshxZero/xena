#pragma once

#include <svg.hpp>

#include <rain.hpp>

namespace Xena {
	class Painter {
		public:
		using Chunks = std::unordered_map<
			Gdiplus::Point,
			std::pair<std::shared_ptr<Chunk>, std::unordered_set<std::size_t>>>;

		private:
		Gdiplus::REAL const DPI_SCALE;

		static inline Gdiplus::REAL const STROKE_WIDTH_DP{2.5f};
		Gdiplus::REAL const STROKE_WIDTH_PX;

		// Chunks are created with a fixed DPI which does not change throughout its
		// lifetime.
		static inline Gdiplus::Point const CHUNK_SIZE_DP{512, 512};
		Gdiplus::Point const CHUNK_SIZE_PX;

		Gdiplus::SolidBrush const GDIPLUS_BLACK_BRUSH{Gdiplus::Color(0xff000000)},
			GDIPLUS_WHITE_BRUSH{Gdiplus::Color(0xffffffff)},
			GDIPLUS_TRANSPARENT_BRUSH{Gdiplus::Color(0x00000000)};
		bool const IS_LIGHT_THEME{Rain::Windows::isLightTheme()};

		HWND hWnd;

		// Const except that start/end cap need to be set.
		Gdiplus::Pen blackPen, whitePen, transparentPen;

		Gdiplus::Point viewportPosition, currentChunk;

		// Maps a chunk coordinate to the chunk, and all path IDs in the chunk.
		Chunks chunks;

		// Maps a path ID to a path, and all chunks the path spans.
		Svg::Paths paths;

		public:
		Svg svg;

		Painter(std::string const &, HWND);

		void rePaint();
		LRESULT onPaint(HWND, WPARAM, LPARAM);

		void addPath(std::shared_ptr<Path const> const &);
		void removePath(std::size_t);

		private:
		Gdiplus::Point getChunkCoordinateForPoint(Gdiplus::PointF const &);

		// Get a chunk if it exists, or create and return it if it doesn't.
		std::pair<std::shared_ptr<Chunk>, std::unordered_set<std::size_t>> &
		getChunkPair(Gdiplus::Point const &);
	};
}
