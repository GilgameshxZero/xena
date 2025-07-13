#pragma once

#include <rain.hpp>

namespace Xena {
	class FileManager {
		public:
		std::vector<std::vector<POINT>> lines;

		FileManager(std::string const &);

		private:
		void load(std::string const &);
	};
}
