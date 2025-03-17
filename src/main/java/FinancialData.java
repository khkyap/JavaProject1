// FinancialData.java
public class FinancialData {
    private double bankOfAmerica;
    private double paypal;
    private double fidelity;
    private double paperCash;

    // Debts
    private double bankDebt;
    private double paypalDebt;

    // Add getters/setters for all fields
    // Add calculate methods:
    public double getBankOfAmerica() { return bankOfAmerica; }
    public void setBankOfAmerica(double amount) { bankOfAmerica = amount; }

    public double getPaypal() { return paypal; }
    public void setPaypal(double amount) { paypal = amount; }

    public double getFidelity() { return fidelity; }
    public void setFidelity(double amount) { fidelity = amount; }

    public double getPaperCash() { return paperCash; }
    public void setPaperCash(double amount) { paperCash = amount; }

    // Debt Getters/Setters
    public double getBankDebt() { return bankDebt; }
    public void setBankDebt(double amount) { bankDebt = amount; }

    public double getPaypalDebt() { return paypalDebt; }
    public void setPaypalDebt(double amount) { paypalDebt = amount; }

    // Calculated totals
    public double getTotalAssets() {
        return bankOfAmerica + paypal + fidelity + paperCash;
    }

    public double getTotalDebt() {
        return bankDebt + paypalDebt;
    }

    public double getNetWorth() {
        return getTotalAssets() - getTotalDebt();
    }
}