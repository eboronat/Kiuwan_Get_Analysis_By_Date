# Kiuwan_Get_Analysis_By_Date
This is a Java program to get information (date, time, lines of code, analysis code) via Kiuwan API REST of all your Baseline and Delivery analyses launched in a parametrizable period of time.

## Running the program

1. The program receives 5 arguments:
- Username: your Kiuwan account username.
- Password: your Kiuwan account password.
- Kiuwan server url: if cloud -> https://kiuwan.com
- Maxdays:
  - 0: get all the analysis done from the beggining of time
  - 1: get all the analysis done from the last 24h.
  - 2: get all the analysis done from the last 48h.
  - 3, 4, 5, ... : get all the analysis done from the last 72h, 96h, 120h, .....
- xlsx path: the path and name of the created file with the information.

### Example
```
java -jar Kiuwan_Get_Analysis_By_Date.jar myusername mypassword https://kiuwan.com 1 "C:\Users\eboronat\analysis.xlsx"
```
You can see attached an example of an output excel file (analysis.xlsx).
