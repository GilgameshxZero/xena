#pragma once

#include <rain.hpp>

namespace Xena {
	class FileManager {
		public:
		FileManager(std::string const &);

		private:
		void load(std::string const &);
	};
}
