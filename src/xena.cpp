#include <xena.hpp>

#include <rain.hpp>

#include <iostream>

int main() {
	HINSTANCE hInstance = GetModuleHandle(NULL);

	std::cout << "Hello world!";

	// Parse command line.
	Rain::String::CommandLineParser parser;

	bool showHelp = false;
	parser.addParser("help", showHelp);
	parser.addParser("h", showHelp);

	try {
		parser.parse(__argc - 1, __argv + 1);
	} catch (...) {
		MessageBox(
			NULL,
			"Failed to parse command-line options. Consider "
			"running with --help.",
			"xena",
			MB_OK);
		return -1;
	}

	if (showHelp) {
		MessageBox(
			NULL,
			"Command-line options (default specified in "
			"parenthesis):\n"
			"--help, -h (off): Display this help message and "
			"exit.\n",
			"xena",
			MB_OK);
		return 0;
	}

	// Register HWND for the tray icon.
	WNDCLASSEX mainWndClass{
		sizeof(WNDCLASSEX),
		NULL,
		NULL,
		0,
		0,
		hInstance,
		NULL,
		NULL,
		NULL,
		NULL,
		"kb-as-mouse-main-wnd",
		NULL};
	Rain::Windows::validateSystemCall(
		RegisterClassEx(&mainWndClass));
	HWND mainWnd =
		Rain::Windows::validateSystemCall(CreateWindowEx(
			WS_EX_NOACTIVATE | WS_EX_TOPMOST | WS_EX_LAYERED |
				WS_EX_TRANSPARENT | WS_EX_TOOLWINDOW,
			mainWndClass.lpszClassName,
			"",
			WS_POPUP,
			0,
			0,
			0,
			0,
			NULL,
			NULL,
			hInstance,
			NULL));

	// Pump message loop for the tray icon.
	BOOL bRet;
	MSG msg;
	while ((bRet = GetMessage(&msg, NULL, 0, 0)) != 0) {
		if (bRet == -1) {
			MessageBox(
				NULL, "GetMessage returned -1.", "xena", MB_OK);
			break;
		} else {
			TranslateMessage(&msg);
			DispatchMessage(&msg);
		}
	}

	Rain::Windows::validateSystemCall(DestroyWindow(mainWnd));
	return 0;
}
