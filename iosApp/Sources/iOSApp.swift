import UIKit
import GoogleMobileAds
import Vocabee

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?
    private let ads = RewardedAdManager()

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        MobileAds.shared.start(completionHandler: nil)
        ads.preload()

        let window = UIWindow(frame: UIScreen.main.bounds)
        window.rootViewController = MainViewControllerKt.MainViewController(presentRewardedAd: { [ads] onResult in
            ads.present { code, message in
                onResult(KotlinInt(int: code), message)
            }
        })
        window.makeKeyAndVisible()
        self.window = window
        return true
    }
}

/// Loads and presents a Google Mobile Ads rewarded ad, reporting the outcome
/// as a plain code (0 = reward earned, 1 = dismissed, 2 = failed) + message.
@MainActor
final class RewardedAdManager: NSObject, FullScreenContentDelegate {
    // Google's official iOS test rewarded unit — swap for a real AdMob unit later.
    private let adUnitID = "ca-app-pub-3940256099942544/1712485313"
    private var rewardedAd: RewardedAd?
    private var onResult: ((Int32, String?) -> Void)?
    private var rewardEarned = false

    func preload() {
        RewardedAd.load(with: adUnitID, request: Request()) { [weak self] ad, _ in
            self?.rewardedAd = ad
            self?.rewardedAd?.fullScreenContentDelegate = self
        }
    }

    func present(onResult: @escaping (Int32, String?) -> Void) {
        self.onResult = onResult
        self.rewardEarned = false

        if let ad = rewardedAd, let root = Self.topViewController() {
            show(ad, from: root)
            return
        }
        // Nothing preloaded — load on demand, then present.
        RewardedAd.load(with: adUnitID, request: Request()) { [weak self] ad, error in
            guard let self = self else { return }
            if let error = error {
                self.finish(2, String(describing: error))
                return
            }
            guard let ad = ad, let root = Self.topViewController() else {
                self.finish(2, "Реклама недоступна")
                return
            }
            self.rewardedAd = ad
            self.show(ad, from: root)
        }
    }

    private func show(_ ad: RewardedAd, from root: UIViewController) {
        ad.fullScreenContentDelegate = self
        ad.present(from: root) { [weak self] in
            self?.rewardEarned = true
        }
    }

    @objc func adDidDismissFullScreenContent(_ ad: any FullScreenPresentingAd) {
        finish(rewardEarned ? 0 : 1, nil)
        rewardedAd = nil
        preload()
    }

    @objc func ad(
        _ ad: any FullScreenPresentingAd,
        didFailToPresentFullScreenContentWithError error: any Error
    ) {
        finish(2, String(describing: error))
        rewardedAd = nil
        preload()
    }

    private func finish(_ code: Int32, _ message: String?) {
        let callback = onResult
        onResult = nil
        callback?(code, message)
    }

    private static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .first { $0.activationState == .foregroundActive } as? UIWindowScene
        let root = scene?.windows.first { $0.isKeyWindow }?.rootViewController
        var top = root
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}
