#include <main.hpp>
#include <xena.hpp>

#include <rain.hpp>

int main() {
	std::string const WINDOW_NAME{"xena"};
	ShowWindow(GetConsoleWindow(), SW_HIDE);

	Rain::String::CommandLineParser parser;
	bool showHelp = false;
	parser.addParser("help", showHelp);
	parser.addParser("h", showHelp);
	try {
		parser.parse(__argc - 1, __argv + 1);
		Rain::Log::verbose("main: ", "Parsed command line options.");
	} catch (...) {
		MessageBox(
			NULL,
			"Failed to parse command-line options. Consider running with --help.",
			WINDOW_NAME.c_str(),
			MB_OK);
		return -1;
	}
	if (showHelp) {
		MessageBox(
			NULL,
			"Command-line options (default specified in parenthesis):\n"
			"--help, -h (off): Display this help message and exit.",
			WINDOW_NAME.c_str(),
			MB_OK);
		return 0;
	}

	Xena::Main();

	BOOL bRet;
	MSG msg;
	while ((bRet = GetMessage(&msg, NULL, 0, 0)) != 0) {
		if (bRet == -1) {
			MessageBox(NULL, "GetMessage returned -1.", WINDOW_NAME.c_str(), MB_OK);
			break;
		} else {
			TranslateMessage(&msg);
			DispatchMessage(&msg);
		}
	}
	return 0;
}
