# MILESTONE 1 DEMONSTRATION

1. Open the desktop application and create accounts for Ana and Ben using `ana@example.test` and `ben@example.test`. Enter a password of at least 12 characters for each demo account.
2. Sign in as Ana. In **Lending**, select **Propose a loan**. Use Ben's email, choose **Money**, enter `250.00` and `PHP`, select the dates, acknowledge the terms and save.
3. Sign out and sign in as Ben. In **Borrowing**, open Ana's proposal and show the amount, parties, dates and **Pending terms** status.
4. As Ben, select **Request a loan**. Use Ana's email, choose **Item** and enter `Black umbrella`. Enter the dates, acknowledge the terms and save.
5. Try an item proposal with a due date before the start date. The form displays the validation error and keeps the entered details so you can correct them.

## Code to show

| File | Responsibility |
| --- | --- |
| `MilestoneWindow.java` | Desktop forms, account session and proposal views |
| `AccountService.java` | Registration, password hashing and sign-in |
| `LoanService.java` | Validates, stores and reads pending proposals for their participants |
| `schema.sql` | Defines the original account, loan and terms tables |
| `MilestoneDemo.java` | Runs the same milestone operations with sample data on GitHub |
