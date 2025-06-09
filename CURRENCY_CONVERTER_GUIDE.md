# Currency Symbol Converter Implementation Guide

This guide explains how the currency symbol converter feature works in BudgetBuddy and how to use it.

## Overview

The currency symbol converter allows users to:
- Select their preferred currency from a list of supported currencies
- View all amounts throughout the app with their selected currency symbol
- **No exchange rate conversion** - only the symbol changes (€100 stays €100, not converted to equivalent USD)

## Architecture

### Core Components

1. **`CurrencyConverter`** - Main service for currency symbol formatting
2. **`UserPreferencesManager`** - Handles currency preference storage
3. **`SettingsViewModel` & `SettingsFragment`** - UI for currency selection
4. **Updated UI Components** - All fragments now use the converter for displaying amounts

### Supported Currencies

- **USD** ($) - US Dollar
- **EUR** (€) - Euro
- **GBP** (£) - British Pound
- **CAD** (C$) - Canadian Dollar
- **AUD** (A$) - Australian Dollar
- **JPY** (¥) - Japanese Yen
- **CNY** (¥) - Chinese Yuan
- **INR** (₹) - Indian Rupee
- **ZAR** (R) - South African Rand (legacy support)

## How It Works

### 1. Symbol-Only Formatting
- User selects their preferred currency in Settings
- All amounts are displayed with the selected currency symbol
- **No conversion happens**: $100 becomes €100, £100, ₹100, etc.
- Values remain exactly the same, only the symbol changes

### 2. Data Storage
- All amounts are stored as-is in the database (no base currency)
- User's currency preference is stored in SharedPreferences
- Simple and fast - no API calls needed

### 3. Display Process
```
Database Amount → CurrencyConverter.formatAmount() → Display with Selected Symbol
```

## Implementation

### Adding Currency Support to a Fragment

1. **Inject CurrencyConverter**:
```kotlin
@AndroidEntryPoint
class YourFragment : Fragment() {
    @Inject
    lateinit var currencyConverter: CurrencyConverter
}
```

2. **Format amounts using the converter**:
```kotlin
// Instead of hardcoded formatting:
val oldFormat = "R${String.format("%.2f", amount)}"

// Use the converter:
val newFormat = currencyConverter.formatAmount(amount)
```

3. **For Adapters, pass converter as parameter**:
```kotlin
class YourAdapter(
    private val onItemClicked: (Item) -> Unit,
    private val currencyConverter: CurrencyConverter
) : ListAdapter<Item, ViewHolder>(DiffCallback()) {
    
    fun bind(item: Item) {
        binding.amountTextView.text = currencyConverter.formatAmount(item.amount)
    }
}

// In Fragment:
adapter = YourAdapter(::onItemClicked, currencyConverter)
```

## User Experience

### Changing Currency

1. User goes to **Settings**
2. Taps **Currency** option
3. Selects from available currencies with symbols: "USD ($)", "EUR (€)", etc.
4. App automatically:
   - Updates the preference
   - Updates all displayed amounts with new symbol
   - Shows confirmation message

### Currency Display Examples

- **Budget Overview**: "€125.50 / €500.00" instead of "R125.50 / R500.00"
- **Expense Lists**: "$24.99" → "£24.99" → "¥24.99" (same number, different symbol)
- **Reports**: Charts and summaries use selected currency symbol
- **Input Fields**: Users see their preferred currency symbol

## Technical Details

### Performance Benefits

- **Ultra Fast**: No API calls or network requests
- **Works Offline**: Always available, no internet dependency
- **Simple**: Just string formatting with different symbols
- **Reliable**: No exchange rate errors or cache issues

### Error Handling

- **Invalid Currency**: Defaults to USD ($)
- **Missing Preference**: Falls back to USD
- **Symbol Not Found**: Uses "$" as fallback

## Code Examples

### Basic Usage
```kotlin
// Inject in Fragment/Activity
@Inject
lateinit var currencyConverter: CurrencyConverter

// Format amounts
val amount = BigDecimal("123.45")
val formatted = currencyConverter.formatAmount(amount) // "€123.45" if EUR selected

// Get current currency info
val currency = currencyConverter.getSelectedCurrency() // "EUR"
val symbol = currencyConverter.getCurrencySymbol() // "€"
```

### Adapter Integration
```kotlin
class ExpenseAdapter(
    private val onItemClicked: (Expense) -> Unit,
    private val currencyConverter: CurrencyConverter
) : ListAdapter<ExpenseListItem, RecyclerView.ViewHolder>() {
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.amountTextView.text = currencyConverter.formatAmount(item.amount)
    }
}
```

## Migration Guide

### Updating Existing Code

1. **Replace hardcoded currency formatting**:
```kotlin
// Old:
binding.amountTextView.text = "R${String.format("%.2f", amount)}"

// New:
binding.amountTextView.text = currencyConverter.formatAmount(amount)
```

2. **Update adapters to accept CurrencyConverter**:
```kotlin
// Old constructor:
class MyAdapter(private val onItemClicked: (Item) -> Unit)

// New constructor:
class MyAdapter(
    private val onItemClicked: (Item) -> Unit,
    private val currencyConverter: CurrencyConverter
)

// Usage in Fragment:
adapter = MyAdapter(::onItemClicked, currencyConverter)
```

3. **Inject CurrencyConverter in Fragments**:
```kotlin
@AndroidEntryPoint
class YourFragment : Fragment() {
    @Inject
    lateinit var currencyConverter: CurrencyConverter
}
```

## Testing

### Manual Testing

1. **Change Currency**:
   - Go to Settings → Currency
   - Select "EUR (€)"
   - Verify all amounts show "€" symbol instead of "R"
   - Values should remain the same (€100, not €85)

2. **Multiple Currency Changes**:
   - Switch between different currencies
   - Verify symbols update immediately
   - Confirm amounts stay the same

## Maintenance

### Adding New Currencies

1. Add to `currencySymbols` map in `CurrencyConverter`:
```kotlin
private val currencySymbols = mapOf(
    // ... existing currencies
    "SEK" to "kr", // Swedish Krona
    "NOK" to "kr"  // Norwegian Krone
)
```

2. Currency automatically appears in Settings dropdown

### Removing Currencies

1. Remove from `currencySymbols` map
2. Handle migration for users who had that currency selected (defaults to USD)

## Benefits of This Approach

1. **Simplicity**: No complex exchange rate logic
2. **Speed**: Instant symbol changes, no loading
3. **Reliability**: Never breaks due to API issues
4. **User-Friendly**: Users see familiar symbols for their region
5. **Privacy**: No external API calls to track usage
6. **Offline**: Always works without internet

## When to Use Exchange Rates

If you later need actual currency conversion:
- Add exchange rate API integration
- Keep this symbol-only mode as a "display preference"
- Let users choose between "Symbol Only" and "Convert Values"

---

This symbol-only approach provides the core benefit of currency localization (familiar symbols) without the complexity and potential issues of exchange rate conversion. 