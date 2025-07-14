#pragma once

#include <chunk.hpp>
#include <compoundPath.hpp>
#include <fileManager.hpp>
#include <point.hpp>

#include <rain.hpp>

namespace Xena {
	class WindowManager {
		private:
		static inline double const CHUNK_SIZE_SCALE{0.25};
		// Point const CHUNK_SIZE;

		Point viewportOffset, currentChunk;
		std::unordered_map<std::size_t, CompoundPath> paths;
		std::unordered_map<Point, Chunk> chunks;

		public:
		HWND hWnd;
		FileManager fileManager;

		WindowManager(std::string const &);

		void redraw();
		void onPaint(HDC);
	};
}
