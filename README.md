# Understanding Venture Capital Boom During COVID-19 (2020-2021)

The period of near-zero interest rates during the COVID-19 pandemic significantly fueled venture capital (VC) investments. Let's break this down from first principles with a deep dive into the underlying economic concepts, mathematics, and core relationships.

## 1. Core Economic Relationship: Interest Rates & Investment Returns

### Risk-Free Rate Fundamentals
- Treasury bonds are considered "risk-free" investments.
- The Federal Reserve slashed rates to near-zero (e.g., Fed Rate = 0.25%), causing a 10-year Treasury bond yield to drop to ~1.5%.
- This low yield became the baseline for all other investments.

**Mathematical Relationship:**
$$
\text{Expected Return} = \text{Risk-Free Rate} + \text{Risk Premium}
$$

**Example:**
- Risk-free rate: 1.5%
- VC risk premium: 25-30%
- Expected VC return: 26.5-31.5%

---

## 2. Cost of Capital Calculations

The Weighted Average Cost of Capital (WACC) incorporates equity and debt costs, influenced heavily by interest rates.

**Formula:**
$$
\text{WACC} = \left( \frac{E}{V} \times R_e \right) + \left( \frac{D}{V} \times R_d \times (1-T) \right)
$$
Where:
- \(E\): Equity value
- \(D\): Debt value
- \(V\): Total value \((E + D)\)
- \(R_e\): Cost of equity
- \(R_d\): Cost of debt
- \(T\): Tax rate

**Pre-COVID Example:**
- Cost of debt: 5%
- Tax rate: 21%
- Equity/Total Value: 70%
- Cost of equity: 15%

$$
\text{WACC} = (0.7 \times 15\%) + (0.3 \times 5\% \times 0.79) = 11.18\%
$$

**During COVID:**
- Cost of debt: 1%
- Other factors unchanged.

$$
\text{WACC} = (0.7 \times 15\%) + (0.3 \times 1\% \times 0.79) = 10.24\%
$$

---

## 3. Present Value Calculations

The Discounted Cash Flow (DCF) formula determines the present value of future cash flows.

**Formula:**
$$
PV = \frac{FV}{(1 + r)^n}
$$
Where:
- \(PV\): Present Value
- \(FV\): Future Value
- \(r\): Discount rate
- \(n\): Number of years

**Example:**
Future value: $100M in 5 years.

- **Pre-COVID (\(r = 5\%\)):**
$$
PV = \frac{100M}{(1.05)^5} = 78.35M
$$
- **During COVID (\(r = 0.25\%\)):**
$$
PV = \frac{100M}{(1.0025)^5} = 98.76M
$$

Lower discount rates significantly increase present value, making future cash flows more attractive.

---

## 4. Investment Allocation Dynamics

### Risk-Return Spectrum (Pre-COVID vs. During COVID)

| Investment Type  | Pre-COVID Returns | COVID Returns |
|-------------------|-------------------|---------------|
| Treasury Bonds    | 2.5%             | 0.25-0.5%     |
| Corporate Bonds   | 4-6%             | 1-3%          |
| Public Equity     | 8-10%            | 8-10%         |
| Private Equity    | 15-20%           | 15-20%        |
| Venture Capital   | 25-30%           | 25-30%        |

---

## 5. Portfolio Optimization

**Modern Portfolio Theory:**

- Portfolio Return:
$$
\text{Portfolio Return} = \Sigma (W_i \times R_i)
$$
- Portfolio Risk:
$$
\text{Portfolio Risk} = \sqrt{\Sigma (W_i^2 \times \sigma_i^2) + 2 \Sigma \Sigma (W_i \times W_j \times \sigma_{ij})}
$$
Where:
- \(W_i\): Weight of asset \(i\)
- \(R_i\): Return of asset \(i\)
- \(\sigma_i\): Standard deviation of asset \(i\)
- \(\sigma_{ij}\): Covariance between assets \(i\) and \(j\)

**COVID Impact:** Investors reallocated portfolios to higher-risk assets like venture capital for better returns.

---

## 6. Money Supply Impact

### M2 Money Supply Growth:
- **Pre-COVID (2019):** ~6% annual growth.
- **During COVID (2020):** ~26% growth.

**Quantity Theory of Money:**
$$
MV = PQ
$$
Where:
- \(M\): Money supply
- \(V\): Velocity of money
- \(P\): Price level
- \(Q\): Real GDP

Higher money supply flooded markets, driving liquidity into VC funds.

---

## 7. Venture Capital Return Expectations

### Power Law Distribution:
- 1-2 companies often return the entire fund.
- Target IRR: 30%+ to compensate for risk.

**Fund Return Math:** For a $100M fund to return 3x:
- Need $300M total return.
- Over 10 years: 11.6% CAGR.
- Adjusting for J-curve and failures:
  - Years 1-3: -20% to -30% IRR
  - Years 4-7: Break-even
  - Years 8-10: Positive returns through exits.

---

## 8. Leverage Effects

When rates are near zero, borrowing becomes almost free, enabling leveraged investments.

**Leverage Return Formula:**
$$
\text{Return with leverage} = (\text{Asset Return} \times \text{Leverage}) - \text{Borrowing Cost}
$$

**Example:**
- **Pre-COVID:** 
$$
10\% \times 2x - 5\% = 15\% \text{ net return}
$$
- **During COVID:**
$$
10\% \times 2x - 0.25\% = 19.75\% \text{ net return}
$$

---

## Summary: The Perfect Storm for VC Investments

1. Low returns in traditional assets.
2. Cheap leverage and abundant liquidity.
3. Higher present values of future cash flows.
4. Portfolio reallocation to riskier, high-return assets.

These combined factors created unprecedented venture capital activity during the COVID-19 pandemic.
