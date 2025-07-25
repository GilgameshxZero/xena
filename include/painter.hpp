#pragma once

#include <chunk.hpp>
#include <path.hpp>

#include <rain.hpp>

namespace Xena {
	class Painter {
		public:
		using PointL = Rain::Algorithm::Geometry::PointL;
		using PointLd = Rain::Algorithm::Geometry::PointLd;
		using Chunks = std::unordered_map<
			PointL,
			std::pair<std::shared_ptr<Chunk>, std::unordered_set<std::size_t>>>;
		using Paths = std::unordered_map<
			std::size_t,
			std::pair<std::shared_ptr<Path const>, std::unordered_set<PointL>>>;

		private:
		Rain::Windows::Window &window;

		public:
		long double const DP_TO_PX;

		private:
		static inline long double const STROKE_WIDTH_DP{2.5l};

		public:
		long double const STROKE_WIDTH_PX;

		private:
		static inline long double const PATH_MIN_DELTA_DP{2.0l};
		long double const PATH_MIN_DELTA_PX;

		// Chunks are created with a fixed DPI which does not change throughout its
		// lifetime.
		static inline PointL const CHUNK_SIZE_DP{512l, 512l};
		PointL const CHUNK_SIZE_PX;

		bool const IS_LIGHT_THEME{Rain::Windows::isLightTheme()};

		PointL size;
		Rain::Windows::DeviceContext dc;
		Rain::Windows::DeviceContextMemory tentativeDc;
		bool isTentativeDirty{false};
		Rain::Windows::Bitmap tentativeBitmap;
		Rain::Windows::SolidBrush const backgroundBrush{
			IS_LIGHT_THEME ? 0x00ffffff : 0x00000000};
		Rain::Windows::SolidPen drawPen, erasePen, tentativeDrawPen;

		PointL viewportPosition, currentChunk;

		// Maps a chunk coordinate to the chunk, and all path IDs in the chunk.
		Chunks chunks;

		// Maps a path ID to a path, and all chunks the path spans.
		Paths paths;

		public:
		Painter(std::string const &, Rain::Windows::Window &);
		~Painter();

		void rePaint();
		LRESULT onPaint(WPARAM, LPARAM);

		void addPath(std::shared_ptr<Path const> const &);
		void removePath(std::size_t);
		Paths const &getPaths();
		Chunks const &getChunks();

		void updateViewportPosition(PointL const &);
		PointL const &getViewportPosition();

		void tentativeClear();
		void tentativeMoveTo(PointL const &);
		void tentativeLineTo(PointL const &);

		template <typename PrecisionType>
		PointL getChunkForPoint(
			Rain::Algorithm::Geometry::Point<PrecisionType> const &);

		private:
		// Get a chunk if it exists, or create and return it if it doesn't.
		std::pair<std::shared_ptr<Chunk>, std::unordered_set<std::size_t>> &
		getChunkPair(PointL const &);
	};
}
