#include <xena.hpp>
#include <xenaProc.hpp>

#include <rain.hpp>

int main() {
	std::string const WINDOW_NAME{"xena"};

	HINSTANCE hInstance = GetModuleHandle(NULL);

	Rain::String::CommandLineParser parser;
	bool showHelp = false;
	parser.addParser("help", showHelp);
	parser.addParser("h", showHelp);
	try {
		parser.parse(__argc - 1, __argv + 1);
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

	// Register HWND for the tray icon.
	WNDCLASSEX mainWndClass{
		sizeof(WNDCLASSEX),
		CS_HREDRAW | CS_VREDRAW,
		Xena::xenaProc,
		0,
		0,
		hInstance,
		NULL,
		LoadCursor(NULL, IDC_ARROW),
		(HBRUSH)(COLOR_WINDOW + 1),
		NULL,
		WINDOW_NAME.c_str(),
		NULL};
	Rain::Windows::validateSystemCall(RegisterClassEx(&mainWndClass));
	HWND mainWnd{Rain::Windows::validateSystemCall(CreateWindowEx(
		NULL,
		mainWndClass.lpszClassName,
		WINDOW_NAME.c_str(),
		WS_OVERLAPPEDWINDOW,
		CW_USEDEFAULT,
		CW_USEDEFAULT,
		CW_USEDEFAULT,
		CW_USEDEFAULT,
		NULL,
		NULL,
		hInstance,
		NULL))};
	ShowWindow(mainWnd, SW_NORMAL);
	UpdateWindow(mainWnd);

	// Pump message loop for the tray icon.
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

	Rain::Windows::validateSystemCall(DestroyWindow(mainWnd));
	return 0;
}
