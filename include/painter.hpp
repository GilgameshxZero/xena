#pragma once

#include <svg.hpp>

#include <rain.hpp>

namespace Xena {
	class Painter {
		public:
		using Chunks = std::unordered_map<
			Gdiplus::Point,
			std::pair<std::shared_ptr<Chunk>, std::unordered_set<std::size_t>>>;

		long double const DP_TO_PX;

		private:
		static inline long double const STROKE_WIDTH_DP{2.5f};
		long double const STROKE_WIDTH_PX;

		// Chunks are created with a fixed DPI which does not change throughout its
		// lifetime.
		static inline Gdiplus::Point const CHUNK_SIZE_DP{512, 512};
		Gdiplus::Point const CHUNK_SIZE_PX;

		bool const IS_LIGHT_THEME{Rain::Windows::isLightTheme()};

		HWND const hWnd;
		POINT size;
		HDC const hDc, hTentativeDc;
		bool isTentativeDirty;
		HBITMAP const hTentativeBitmap, hOrigBitmap;
		HBRUSH const hBackgroundBrush{Rain::Windows::validateSystemCall(
			CreateSolidBrush(IS_LIGHT_THEME ? 0x00ffffff : 0x00000000))};
		HPEN const hDrawPen, hErasePen, hTentativeDrawPen, hOrigPen;

		Gdiplus::Point viewportPosition, currentChunk;

		// Maps a chunk coordinate to the chunk, and all path IDs in the chunk.
		Chunks chunks;

		// Maps a path ID to a path, and all chunks the path spans.
		Svg::Paths paths;

		public:
		Svg svg;

		Painter(std::string const &, HWND);
		~Painter();

		void rePaint();
		LRESULT onPaint(HWND, WPARAM, LPARAM);

		void addPath(std::shared_ptr<Path const> const &);
		void removePath(std::size_t);

		void updateViewportPosition(Gdiplus::Point const &);
		Gdiplus::Point const &getViewportPosition();

		void tentativeClear();
		void tentativeMoveTo(POINT const &);
		void tentativeLineTo(POINT const &);

		private:
		Gdiplus::Point getChunkCoordinateForPoint(Path::Point const &);

		// Get a chunk if it exists, or create and return it if it doesn't.
		std::pair<std::shared_ptr<Chunk>, std::unordered_set<std::size_t>> &
		getChunkPair(Gdiplus::Point const &);
	};
}
