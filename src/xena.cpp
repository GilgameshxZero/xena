#include <main-window.hpp>
#include <xena.hpp>

#include <rain.hpp>

int main() {
	return Rain::Error::consumeThrowable(
		[]() {
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

			Gdiplus::GdiplusStartupInput gdiplusStartupInput;
			ULONG_PTR gdiplusToken;
			GdiplusStartup(&gdiplusToken, &gdiplusStartupInput, 0);
			Rain::Windows::validateSystemCall(EnableMouseInPointer(TRUE));
			// DPI awareness can also be set in the manifest, but we set it here.
			Rain::Windows::validateSystemCall(SetThreadDpiAwarenessContext(
				DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2));
			Xena::MainWindow mainWindow(
				nonKeyedArguments.empty() ? "xena.svg" : nonKeyedArguments.front());

			BOOL bRet;
			MSG msg;
			while ((bRet = GetMessage(&msg, NULL, 0, 0)) != 0) {
				if (bRet == -1) {
					MessageBox(
						NULL, "GetMessage returned -1.", WINDOW_NAME.c_str(), MB_OK);
					break;
				} else {
					TranslateMessage(&msg);
					DispatchMessage(&msg);
				}
			}

			Gdiplus::GdiplusShutdown(gdiplusToken);
			return 0;
		},
		"main")();
}
