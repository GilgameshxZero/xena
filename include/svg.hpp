#pragma once

#include <chunk.hpp>
#include <path.hpp>
#include <point-ll.hpp>

#include <rain.hpp>

namespace Xena {
	class Svg {
		private:
		// Svgs are created with a fixed DPI which does not change throughout its
		// lifetime.
		static inline PointLl const CHUNK_SIZE_DP{512, 512};
		PointLl const CHUNK_SIZE_PX;

		std::string filePath;

		PointLl viewportOffset, currentChunk;
		std::unordered_map<std::size_t, Path> paths;
		std::unordered_map<PointLl, Chunk> chunks;

		public:
		Svg(std::string const &, UINT);

		void load();
		void save();
	};
}
