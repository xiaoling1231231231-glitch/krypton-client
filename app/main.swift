import Cocoa
import WebKit

let SERVER_PORT = 5757
let LAUNCHER_DIR = "/Users/sophies/Documents/Default Project/krypton-client/launcher"

final class LauncherServer {
    private var process: Process?
    private(set) var isRunning = false

    func start(port: Int, completion: @escaping (Bool) -> Void) {
        if isUp(port: port) { isRunning = true; completion(true); return }

        let proc = Process()
        proc.executableURL = URL(fileURLWithPath: "/usr/bin/env")
        proc.arguments = ["node", LAUNCHER_DIR + "/server.js"]
        proc.environment = ProcessInfo.processInfo.environment.merging(["PORT": String(port)]) { _, new in new }
        proc.standardOutput = FileHandle.nullDevice
        proc.standardError = FileHandle.nullDevice

        do {
            try proc.run()
        } catch {
            completion(false); return
        }
        process = proc

        var attempts = 0
        func poll() {
            if self.isUp(port: port) {
                self.isRunning = true
                completion(true)
            } else if attempts > 40 {
                completion(false)
            } else {
                attempts += 1
                DispatchQueue.global().asyncAfter(deadline: .now() + 0.25, execute: poll)
            }
        }
        poll()
    }

    func stop() {
        if let p = process, p.isRunning {
            p.terminate()
        }
        process = nil
        isRunning = false
    }

    private func isUp(port: Int) -> Bool {
        let sock = socket(AF_INET, SOCK_STREAM, 0)
        guard sock >= 0 else { return false }
        defer { close(sock) }
        var addr = sockaddr_in()
        addr.sin_family = sa_family_t(AF_INET)
        addr.sin_port = in_port_t(port).bigEndian
        addr.sin_addr.s_addr = inet_addr("127.0.0.1")
        let connected = withUnsafePointer(to: &addr) {
            $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                connect(sock, $0, socklen_t(MemoryLayout<sockaddr_in>.size))
            }
        }
        return connected == 0
    }
}

final class ViewController: NSViewController {
    private let webView = WKWebView()
    private let server = LauncherServer()
    private var serverUp = false

    override func loadView() {
        let v = NSView(frame: NSRect(x: 0, y: 0, width: 1080, height: 720))
        webView.translatesAutoresizingMaskIntoConstraints = false
        webView.allowsMagnification = true
        webView.customUserAgent = nil
        webView.navigationDelegate = self
        v.addSubview(webView)
        NSLayoutConstraint.activate([
            webView.leadingAnchor.constraint(equalTo: v.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: v.trailingAnchor),
            webView.topAnchor.constraint(equalTo: v.topAnchor),
            webView.bottomAnchor.constraint(equalTo: v.bottomAnchor)
        ])
        self.view = v
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        server.start(port: SERVER_PORT) { ok in
            DispatchQueue.main.async {
                self.serverUp = ok
                if ok {
                    self.webView.load(URLRequest(url: URL(string: "http://localhost:\(SERVER_PORT)")!))
                }
            }
        }
    }

    func stopServer() { server.stop() }
}

extension ViewController: WKNavigationDelegate {
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction,
                 decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if let url = navigationAction.request.url,
           url.host != "localhost" && url.scheme != "http" {
            NSWorkspace.shared.open(url)
            decisionHandler(.cancel)
        } else {
            decisionHandler(.allow)
        }
    }
}

final class AppDelegate: NSObject, NSApplicationDelegate {
    private var window: NSWindow!
    private var viewController: ViewController!

    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApp.setActivationPolicy(.regular)

        viewController = ViewController()

        window = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 1080, height: 720),
            styleMask: [.titled, .closable, .miniaturizable, .resizable],
            backing: .buffered,
            defer: false
        )
        window.title = "Krypton Client"
        window.contentViewController = viewController
        window.minSize = NSSize(width: 800, height: 560)
        window.center()
        window.makeKeyAndOrderFront(nil)

        NSApp.activate(ignoringOtherApps: true)

        let menu = NSMenu()
        let appItem = NSMenuItem()
        menu.addItem(appItem)
        let appMenu = NSMenu()
        appMenu.addItem(NSMenuItem(title: "Quit Krypton", action: #selector(NSApplication.terminate(_:)), keyEquivalent: "q"))
        appItem.submenu = appMenu
        NSApp.mainMenu = menu
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
        return true
    }

    func applicationWillTerminate(_ notification: Notification) {
        viewController?.stopServer()
    }
}

let app = NSApplication.shared
let appDelegate = AppDelegate()
app.delegate = appDelegate
app.run()