#include <fileManager.hpp>

namespace Xena {
	FileManager::FileManager(std::string const &fileToLoad) {
		this->load(fileToLoad);
	}

	void FileManager::load(std::string const &fileToLoad) {
		std::ifstream in(fileToLoad, std::ios::binary);
		std::stack<std::string> tags;
		std::string buffer;
		in >> buffer;
		while (!in.eof()) {
			if (buffer[0] != '<') {
				throw std::ios_base::failure("Invalid starting tag.");
			}
			if (buffer[1] == '/') {
				if (tags.top() != buffer.substr(2)) {
					throw std::ios_base::failure("Invalid closing tag.");
				}
				tags.pop();
			}
		}
		in.close();
		Rain::Log::verbose("FileManager::FileManager: Loaded ", fileToLoad, ".");
	}
}
