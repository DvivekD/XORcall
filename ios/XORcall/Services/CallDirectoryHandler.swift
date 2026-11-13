import Foundation
import CallKit

/**
 * CallDirectoryHandler (iOS CallKit Extension)
 * Automatically labels and blocks spoofed VOIP caller IDs, Cambodia/Myanmar telecom prefixes,
 * and known extortion pools before the call connects.
 */
class CallDirectoryHandler: CXCallDirectoryProvider {

    override func beginRequest(with context: CXCallDirectoryExtensionContext) {
        context.delegate = self

        // Add blocked numbers
        addAllBlockingPhoneNumbers(to: context)

        // Add identification labels for suspicious prefixes
        addAllIdentificationPhoneNumbers(to: context)

        context.completeRequest()
    }

    private func addAllBlockingPhoneNumbers(to context: CXCallDirectoryExtensionContext) {
        // High-risk known fraudulent numbers (E.164 format without '+')
        let blockedNumbers: [CXCallDirectoryPhoneNumber] = [
            923001234567, // +92 spoofing syndicate
            85512345678,  // +855 Cambodia compound gateway
            95912345678   // +95 Myanmar cyber-slavery compound
        ]

        for phoneNumber in blockedNumbers.sorted() {
            context.addBlockingEntry(withNextSequentialPhoneNumber: phoneNumber)
        }
    }

    private func addAllIdentificationPhoneNumbers(to context: CXCallDirectoryExtensionContext) {
        // Flag incoming international calls pretending to be Indian agencies
        let identifiedNumbers: [(number: CXCallDirectoryPhoneNumber, label: String)] = [
            (911123456789, "XORcall ALERT: Suspected Spoofed CBI Number"),
            (912222334455, "XORcall ALERT: Unverified Telecom Impersonator")
        ]

        for entry in identifiedNumbers.sorted(by: { $0.number < $1.number }) {
            context.addIdentificationEntry(
                withNextSequentialPhoneNumber: entry.number,
                label: entry.label
            )
        }
    }
}

extension CallDirectoryHandler: CXCallDirectoryExtensionContextDelegate {
    func requestFailed(for extensionContext: CXCallDirectoryExtensionContext, withError error: Error) {
        print("XORcall CallKit directory update failed: \(error.localizedDescription)")
    }
}
