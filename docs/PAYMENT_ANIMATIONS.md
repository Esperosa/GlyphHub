# Payment Animations

Date: 2026-06-07

GlyphHub payment output is a visual/status feature only.

## Implemented

- `PaymentToyModule` shows a manual payment-active visual.
- Manual success mode can show a checkmark.
- Status event types exist for payment/wallet-active visuals.

## Explicit Limits

- GlyphHub does not detect confirmed transaction success.
- Success visuals are only manual/test visuals unless a future NFC event is explicitly received.
- No Accessibility Service is implemented.
- UsageStats Wallet foreground detection is not implemented in this pass.
- NFC trigger is not implemented in this pass.
- The app does not request `NFC` or `PACKAGE_USAGE_STATS` until those trigger paths exist.

## Future Safe Triggers

- Manual app/widget/web trigger.
- NFC tag event if Android delivers a real NFC event to the app.
- Wallet foreground detection only after the user grants Usage Access, labelled as "Wallet active", not "Payment successful".
