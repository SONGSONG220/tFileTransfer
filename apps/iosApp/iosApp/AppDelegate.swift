import UIKit
import appShareKit

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        let win = UIWindow(frame: UIScreen.main.bounds)
        let vc = IosEntry().rootViewController()
        win.rootViewController = vc
        win.makeKeyAndVisible()
        window = win
        return true
    }
}

