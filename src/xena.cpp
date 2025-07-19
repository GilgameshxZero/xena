#include <main-window.hpp>
#include <xena.hpp>

#include <rain.hpp>

int main() {
	try {
		std::string const WINDOW_NAME{"xena"};
		ShowWindow(GetConsoleWindow(), SW_HIDE);

		Rain::String::CommandLineParser parser;
		std::vector<std::string> nonKeyedArguments;
		bool showHelp = false;
		parser.addParser("help", showHelp);
		parser.addParser("h", showHelp);
		try {
			parser.parse(__argc - 1, __argv + 1, nonKeyedArguments);
		} catch (...) {
			MessageBox(
				NULL,
				"Failed to parse command-line options. Consider running with --help.",
				WINDOW_NAME.c_str(),
				MB_OK);
			return -1;
		}
		Rain::Log::verbose("main: ", "Parsed command line options.");
		if (showHelp) {
			MessageBox(
				NULL,
				"Command-line options (default specified in parenthesis):\n"
				"--help, -h (off): Display this help message and exit.",
				WINDOW_NAME.c_str(),
				MB_OK);
			return 0;
		}

		// DPI awareness can also be set in the manifest, but we set it here.
		Rain::Windows::validateSystemCall(
			SetThreadDpiAwarenessContext(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2));
		Rain::Windows::validateSystemCall(EnableMouseInPointer(TRUE));
		Xena::MainWindow mainWindow(
			nonKeyedArguments.empty() ? "xena.svg" : nonKeyedArguments.front());
		Rain::Windows::runMessageLoop();
		return 0;
	} catch (std::exception const &exception) {
		std::cerr << exception.what();
		return -1;
	}
}
