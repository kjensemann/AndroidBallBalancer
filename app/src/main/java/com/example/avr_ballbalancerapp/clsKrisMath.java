package com.example.avr_ballbalancerapp;

public class clsKrisMath {

    public double[] khjCubicSplineEngDetails(double x_value, double[] x_vect, double[] y_vect) {
        double y_value_ans = 0, a, b, c, d;
        double khjCubicSplineEngDouble;

        double ansVar[] = new double[5];
        String ansVarStr[] = new String[5];

        /*
        ansVar Data:    [0] - Answer Value, and Answer Comment
                        [1] - Iteration Count, Type
                        [2] - Answer Range lower (int), comment
                        [3] - Answer Range Upper (int), comment
         */

        int b_int_Low, b_int_High, b_int_Act;
        int bis_Count = 0;
        int xArrayBoundCount = 0;
        int x_vect_len;
        int n_test = 0;

        double[] dy_dx = new double[2];
        double[] dy_d2x = new double[2];

        x_vect_len = x_vect.length-1;
        int i = 0;
        int n = 0;

        //'------------------- LINEÆR EKSTRAPOLASJON UTENFOR MÅLEPUNKTER --------------------------
        if (x_value <= x_vect[0]) {
            //'Sjekker om x er lavere enn laveste (første) x-verdi Y = Bx+A
            n = 0;
            if (x_vect[n + 1] - x_vect[n] == 0) {
                b = 1E+30;
            } else {
                b = (y_vect[n + 1] - y_vect[n]) / (x_vect[n + 1] - x_vect[n]);
                a = y_vect[n] - b * x_vect[n];

                khjCubicSplineEngDouble = b * x_value + a;

                ansVar[0] = khjCubicSplineEngDouble; //Main Value
                ansVarStr[0] = "x outside start of range - Linear extrapolation"; //STATUS
                ansVar[1] = 0; //Iterations
                ansVarStr[1]="No Iterations";
                ansVar[2] = 0; //Range Lower
                ansVarStr[2] = "Before start of range"; //Range Lower
                ansVar[3] = 0; //Range Upper
                ansVarStr[3] = "Not appliccable"; //Range Lower

                return ansVar;
            }
        } else if (x_value > x_vect_len) {

            n = x_vect_len;
            if (x_vect[n] - x_vect[n - 1] == 0) {
                b = 1E+30;
            } else {
                b = (y_vect[n] - y_vect[n - 1]) / (x_vect[n] - x_vect[n - 1]);
                a = y_vect[n] - b * x_vect[n];
                khjCubicSplineEngDouble = b * x_value + a;
                ansVar[0] = khjCubicSplineEngDouble;
                ansVarStr[0] = "x outside end of range - Linear extrapolation";
                ansVar[1] = 0; //Iterations
                ansVarStr[1]="No Iterations";
                ansVar[2] = 0; //Range Lower
                ansVarStr[2] = "Not Appliccable"; //Range Lower
                ansVar[3] = x_vect_len; //Range Upper
                ansVarStr[3] = "Outside end of range"; //Range Lower


                return ansVar;

            }
        }


        //'------------------- ENGINEERING SPLINE INNENFOR MÅLEPUNKTER --------------------------
        //'Finner i hvilket intervall vi befinner oss i :

        i = 0;
        b_int_Low = 0;
        b_int_High = x_vect.length-1; //'Length of stepwise inclining x_vector


        if (b_int_High > 10) { //'USES BRACKETING IF ARRAY IS LARGE..

            ansVarStr[4] = "bracketing > 10";

            do {

                ansVar[1]=i;
                ansVarStr[1] = "bracket Cnt: " + String.valueOf(i);

                if (x_value < x_vect[b_int_High] || x_value > x_vect[b_int_Low]) {

                    b_int_Act = (int) Math.ceil((double) ((b_int_High + b_int_Low) / 2));
                    //'Finner midten av array (deler array i to, for så å finne ut om valgt verdi er midt i mellom)

                    if (b_int_High - b_int_Low == 1) {
                        //'We have found our point
                        n_test = b_int_High;
                        break;
                    } else if (x_value == x_vect[b_int_Act]) {
                        n_test = b_int_Act + 1;
                        break;
                    } else if (x_value < x_vect[b_int_Act]) // 'And x_value >= x_vect(b_int_Low) Then ' x må ligge i intervallet b_int_act -- > b_int_high
                    {
                        b_int_High = b_int_Act;
                    } else if (x_value >= x_vect[b_int_Act]) // 'And x_value < x_vect(b_int_High) Then
                    {
                        b_int_Low = b_int_Act;
                    }

                }


                i = i + 1;
                bis_Count = bis_Count + 1;


            } while (i < 200); //Loop While

            n = n_test;

            xArrayBoundCount = x_vect.length-1;
            //ansVar[2] = xArrayBoundCount - n;
            ansVar[1] = bis_Count;
            ansVarStr[1] = "Braceting, iterations = " + String.valueOf(bis_Count);
            ansVarStr[2] = "Bracketing, range lower location = " + String.valueOf(n-1);
            ansVar[2] = n-1;
            ansVarStr[3] = "Bracketing, range upper location = " + String.valueOf(n);
            ansVar[3] = n;


        } else  // 'IF WE DO NOT HAVE MANY ELEMENTS, THEN STEP SEARCH IS USED
        {
            ansVar[1] = b_int_High;
            ansVarStr[1] = "step search, as array elements are below 10. Array Length: " + String.valueOf(b_int_High) ;

            for (i = 1; i < x_vect_len; i++) {
                if (x_value < x_vect[i]) // 'And x_value > x_vect(i - 1)
                {
                    n = i; //'Setter Intervallnummeret vi befinner oss i
                    ansVarStr[1] = "StepCnt: " + String.valueOf(i);
                    ansVar[1] = n;
                    ansVarStr[2] = "Step Change, range lower location = " + String.valueOf(i-1);
                    ansVar[2] = i-1;
                    ansVarStr[3] = "Step Change, range upper location = " + String.valueOf(i);
                    ansVar[3] = i;

                    break; //Exit for
                }
            }
        }

        i = 0;
        int j;
        //'---------- BEREGNER DE 1. DERIVERTE ----------------------
        for (j = 0; j <= 1; j++) {
            i = n + j - 1; //'i er alltid >=1 fra over
            if (i == 0 || i == x_vect_len) {
                //'Sjekker om vi er på siste intervallet, i så fall er telleren vår ett hakk for langt frem, og vi går over rangen vår..
                dy_dx[j] = 1E+30;
                //'Utfører noen sjekker:
            } else if ((y_vect[i + 1] - y_vect[i]) == 0 || (y_vect[i] - y_vect[i - 1]) == 0) {
                dy_dx[j] = 0;
            } else if (((x_vect[i + 1] - x_vect[i]) / (y_vect[i + 1] - y_vect[i]) + (x_vect[i] - x_vect[i - 1]) / (y_vect[i] - y_vect[i - 1])) == 0) {
                //'Pos PLUS neg slope is 0. Prevent div by zero.
                dy_dx[j] = 0;
            } else if ((y_vect[i + 1] - y_vect[i]) * (y_vect[i] - y_vect[i - 1]) < 0) //'Pos AND neg slope, assume slope = 0 to prevent overshoot
            {
                dy_dx[j] = 0;
            } else {
                dy_dx[j] = 2 / ((((x_vect[i + 1] - x_vect[i]) / (y_vect[i + 1] - y_vect[i])) + ((x_vect[i] - x_vect[i - 1]) / (y_vect[i] - y_vect[i - 1]))));
            }

        }

        //'Sjekker om vi må inn med tilpassede deriverte i første og siste intervall:
        if (n == 1) {
            dy_dx[0] = 3 / 2 * (y_vect[n] - y_vect[n - 1]) / (x_vect[n] - x_vect[n - 1]) - dy_dx[1] / 2;
        } else if (n == x_vect_len) {
            dy_dx[1] = 3 / 2 * (y_vect[n] - y_vect[n - 1]) / (x_vect[n] - x_vect[n - 1]) - dy_dx[0] / 2;
        }

        //'-----------------BEREGNER DE 2.DERIVERTE------------------
        dy_d2x[0] = -2 * (dy_dx[1] + 2 * dy_dx[0]) / (x_vect[n] - x_vect[n - 1]) + 6 * (y_vect[n] - y_vect[n - 1]) / ((x_vect[n] - x_vect[n - 1]) * (x_vect[n] - x_vect[n - 1]));
        dy_d2x[1] = 2 * (2 * dy_dx[1] + dy_dx[0]) / (x_vect[n] - x_vect[n - 1]) - 6 * (y_vect[n] - y_vect[n - 1]) / ((x_vect[n] - x_vect[n - 1]) * (x_vect[n] - x_vect[n - 1]));

        d = (dy_d2x[1] - dy_d2x[0]) / (6 * (x_vect[n] - x_vect[n - 1]));
        c = (x_vect[n] * dy_d2x[0] - x_vect[n - 1] * dy_d2x[1]) / (2 * (x_vect[n] - x_vect[n - 1]));
        b = ((y_vect[n] - y_vect[n - 1]) - c * (x_vect[n] * x_vect[n] - x_vect[n - 1] * x_vect[n - 1]) - d * (x_vect[n] * x_vect[n] * x_vect[n] - x_vect[n - 1] * x_vect[n - 1] * x_vect[n - 1])) / (x_vect[n] - x_vect[n - 1]);
        a = y_vect[n - 1] - b * x_vect[n - 1] - c * x_vect[n - 1] * x_vect[n - 1] - d * x_vect[n - 1] * x_vect[n - 1] * x_vect[n - 1];

        //'1st ORDER DERIVATIVES OF INTERMEDIATE POINTS

        khjCubicSplineEngDouble = d * x_value * x_value * x_value + c * x_value * x_value + b * x_value + a;
        //ansVarStr[2] = "Interval: " + String.valueOf(xArrayBoundCount - n + 1) + " TO " + String.valueOf(xArrayBoundCount - n + 2);
        ansVar[0] = khjCubicSplineEngDouble;
        ansVarStr[0] = "Solution Found";

        return ansVar;
        /*
        ansVar Data:    [0] - Answer Value, and Answer Comment
                        [1] - Iteration Count, Type
                        [2] - Answer Range lower (int), comment
                        [3] - Answer Range Upper (int), comment
         */

    }

} //END FUNCTION


//BY: Kristian Holm Jensen - VBA codes implemented to Java
    /*

    Public Function khjCubicSplineEngDetails(ByVal x_value As Variant, ByVal x_vect_in As Variant, ByVal y_vect_in As Variant) As Variant
        '----------------------------------------------------------------
        '----- FUNKSJON SOM LAGER KUBISK FIT VHA TRE MÅLEPUNKTER --------
        ' http://www.korf.co.uk/spline.pdf
        ' Input: x_value er x_verdi som skal tilegnes en y_verdi
        ' x_vect_in er oppgitt array med verdier på x_aksen (måledata)
        ' y_vect_in er oppgitt array med verdier på y_aksen (måledata)
        ' NB!!! X_Vector må være i STIGENDE rekkefølge! e.g. x --> 0, 1, 3, 9, 12 etc, not 9,5,3,2,1 !!.

        'Array is returned, showing also where solution is found (between which values)

        '----------------------------------------------------------------

        Dim b_int_Low As Integer, b_int_High As Integer, b_int_Act As Integer
        Dim bis_Count As Integer
        Dim n_test As Integer

        Dim xArrayBoundCount As Integer

        Dim x_vect1() ' As Double
        Dim y_Vect1() ' As Double
        Dim i As Integer, n As Integer, j As Integer
        Dim x_vect() As Double
        Dim y_vect() As Double
        Dim a As Double, b As Double, c As Double, d As Double
        Dim dy_dx(0 To 1) As Double
        Dim dy_d2x(0 To 1) As Double

        Dim bracketIntCount As Integer
        Dim intervalNr As Integer
        Dim Status As String

        Dim khjCubicSplineEngDouble As Double

        Dim ansVar(0 To 0, 0 To 4) As Variant


        Application.Calculation = xlCalculationManual ' TO AVOID DELAYS

        CubicSplineCount = CubicSplineCount + 1 'DENNE TELLER HVOR MANGE GANGER FUNKSJONEN ER BRUKT - SE GLOBAL ØVERST I MODULEN

        x_vect1() = x_vect_in.value
        y_Vect1() = y_vect_in.value

        If UBound(x_vect1) = 1 Then 'HVIS VECTORER KOMMER INN HORISONTALE

            ReDim x_vect(UBound(x_vect1, 2) - 1)
            ReDim y_vect(UBound(y_Vect1, 2) - 1)

            If x_vect1(1, 1) - x_vect1(1, 2) < 0 Then 'HVIS X_Vector er oppgitt i Økende rekkefølge, dvs X_vect = [1,2,4,5,5.2,6.1,8] (normalt)
                For i = 0 To UBound(x_vect, 1)
                    x_vect(i) = x_vect1(1, i + 1) 'Lager x og y-vektor
                    y_vect(i) = y_Vect1(1, i + 1)
                Next i
            Else 'Hvis X_vect1 kommer inn i synkende rekkefølge.
                For i = 0 To UBound(x_vect, 1)
                    x_vect(i) = x_vect1(1, UBound(x_vect, 1) - i) 'Lager x og y-vektor
                    y_vect(i) = y_Vect1(1, UBound(x_vect, 1) - i)
                Next i
            End If

        Else 'HVIS VECTORER KOMMER INN VERTIKALT

            ReDim x_vect(UBound(x_vect1, 1) - 1)
            ReDim y_vect(UBound(y_Vect1, 1) - 1)
            xArrayBoundCount = UBound(x_vect1, 1) - 1

            If x_vect1(1, 1) - x_vect1(2, 1) < 0 Then 'Hvis X_vect1 kommer inn i økende rekkefølge

                For i = 0 To UBound(x_vect, 1)
                    x_vect(i) = x_vect1(i + 1, 1)
                    y_vect(i) = y_Vect1(i + 1, 1)
                Next i
            Else 'HVIS X_Vect kommer inn i synkende rekkefølge.
                For i = 0 To UBound(x_vect, 1)
                    x_vect(i) = x_vect1(UBound(x_vect1, 1) - i, 1)
                    y_vect(i) = y_Vect1(UBound(x_vect1, 1) - i, 1)
                Next i
            End If


        End If

        i = 0: n = 0

            '------------------- LINEÆR EKSTRAPOLASJON UTENFOR MÅLEPUNKTER --------------------------
            If x_value <= x_vect(0) Then 'Sjekker om x er lavere enn laveste (første) x-verdi Y = Bx+A
                n = 0
                    If x_vect(n + 1) - x_vect(n) = 0 Then
                        b = 1E+30
                    Else
                        b = (y_vect(n + 1) - y_vect(n)) / (x_vect(n + 1) - x_vect(n))
                        a = y_vect(n) - b * x_vect(n)
                        khjCubicSplineEngDouble = b * x_value + a
                        ansVar(0, 0) = khjCubicSplineEngDouble
                        ansVar(0, 4) = "x outside start of range"
                        khjCubicSplineEngDetails = ansVar
                        Exit Function
                    End If
            ElseIf x_value >= x_vect(UBound(x_vect)) Then
                n = UBound(x_vect)
                    If x_vect(n) - x_vect(n - 1) = 0 Then
                        b = 1E+30
                    Else
                        b = (y_vect(n) - y_vect(n - 1)) / (x_vect(n) - x_vect(n - 1))
                        a = y_vect(n) - b * x_vect(n)
                        khjCubicSplineEngDouble = b * x_value + a
                        ansVar(0, 0) = khjCubicSplineEngDouble
                        ansVar(0, 4) = "x outside end of range"
                        khjCubicSplineEngDetails = ansVar
                        Exit Function
                    End If
            End If

        '------------------- ENGINEERING SPLINE INNENFOR MÅLEPUNKTER --------------------------
            'Finner i hvilket intervall vi befinner oss i :

        i = 0

        b_int_Low = 0
        b_int_High = UBound(x_vect) 'Length of stepwise inclining x_vector


        If b_int_High > 10 Then 'USES BRACKETING IF ARRAY IS LARGE..

            ansVar(0, 4) = "bracketing > 10"

            Do While i < 200
                ansVar(0, 3) = "bracketCnt: " & i
                If x_value < x_vect(b_int_High) And x_value > x_vect(b_int_Low) Then

                b_int_Act = Application.RoundUp((b_int_High + b_int_Low) / 2, 0) 'Finner midten av array (deler array i to, for så å finne ut om valgt verdi er midt i mellom)

                    If b_int_High - b_int_Low = 1 Then 'We have found our point
                        n_test = b_int_High
                        Exit Do
                    ElseIf x_value = x_vect(b_int_Act) Then
                        n_test = b_int_Act + 1
                        Exit Do
                    ElseIf x_value < x_vect(b_int_Act) Then 'And x_value >= x_vect(b_int_Low) Then 'x må ligge i intervallet b_int_act --> b_int_high
                        b_int_High = b_int_Act

                    ElseIf x_value >= x_vect(b_int_Act) Then 'And x_value < x_vect(b_int_High) Then
                       b_int_Low = b_int_Act
                    End If

                End If

                i = i + 1
                bis_Count = bis_Count + 1
                If i > 100 Then
                    'MsgBox "Cubic Spline Eng - Bisection did not find interval, ended in 100 iterations"
                End If

            Loop
        n = n_test
        ansVar(0, 2) = xArrayBoundCount - n

        Else 'IF WE DO NOT HAVE MANY ELEMENTS, THEN STEP SEARCH IS USED
          ansVar(0, 4) = "step search < 10"
            For i = 1 To UBound(x_vect)
                If x_value < x_vect(i) Then 'And x_value > x_vect(i - 1)
                    n = i 'Setter Intervallnummeret vi befinner oss i
                    ansVar(0, 3) = "StepCnt: " & i
                    ansVar(0, 2) = n
                    Exit For
                End If
            Next i
        End If

        i = 0
        '---------- BEREGNER DE 1. DERIVERTE ----------------------
        For j = 0 To 1
            i = n + j - 1 'i er alltid >=1 fra over
                If i = 0 Or i = UBound(x_vect) Then 'Sjekker om vi er på siste intervallet, i så fall er telleren vår ett hakk for langt frem, og vi går over rangen vår..
                dy_dx(j) = 1E+30
                'Utfører noen sjekker:
                ElseIf (y_vect(i + 1) - y_vect(i)) = 0 Or (y_vect(i) - y_vect(i - 1)) = 0 Then
                    dy_dx(j) = 0
                ElseIf ((x_vect(i + 1) - x_vect(i)) / (y_vect(i + 1) - y_vect(i)) + (x_vect(i) - x_vect(i - 1)) / (y_vect(i) - y_vect(i - 1))) = 0 Then
            'Pos PLUS neg slope is 0. Prevent div by zero.
                    dy_dx(j) = 0
                ElseIf (y_vect(i + 1) - y_vect(i)) * (y_vect(i) - y_vect(i - 1)) < 0 Then
            'Pos AND neg slope, assume slope = 0 to prevent overshoot
                    dy_dx(j) = 0
                Else
                 dy_dx(j) = 2 / ((((x_vect(i + 1) - x_vect(i)) / (y_vect(i + 1) - y_vect(i))) + ((x_vect(i) - x_vect(i - 1)) / (y_vect(i) - y_vect(i - 1)))))
            End If
        Next j

        'Sjekker om vi må inn med tilpassede deriverte i første og siste intervall:
        If n = 1 Then
            dy_dx(0) = 3 / 2 * (y_vect(n) - y_vect(n - 1)) / (x_vect(n) - x_vect(n - 1)) - dy_dx(1) / 2
        ElseIf n = UBound(x_vect) Then

               dy_dx(1) = 3 / 2 * (y_vect(n) - y_vect(n - 1)) / (x_vect(n) - x_vect(n - 1)) - dy_dx(0) / 2
        End If

        '-----------------BEREGNER DE 2.DERIVERTE------------------
        dy_d2x(0) = -2 * (dy_dx(1) + 2 * dy_dx(0)) / (x_vect(n) - x_vect(n - 1)) + 6 * (y_vect(n) - y_vect(n - 1)) / (x_vect(n) - x_vect(n - 1)) ^ 2
        dy_d2x(1) = 2 * (2 * dy_dx(1) + dy_dx(0)) / (x_vect(n) - x_vect(n - 1)) - 6 * (y_vect(n) - y_vect(n - 1)) / (x_vect(n) - x_vect(n - 1)) ^ 2

        d = (dy_d2x(1) - dy_d2x(0)) / (6 * (x_vect(n) - x_vect(n - 1)))
        c = (x_vect(n) * dy_d2x(0) - x_vect(n - 1) * dy_d2x(1)) / (2 * (x_vect(n) - x_vect(n - 1)))
        b = ((y_vect(n) - y_vect(n - 1)) - c * (x_vect(n) ^ 2 - x_vect(n - 1) ^ 2) - d * (x_vect(n) ^ 3 - x_vect(n - 1) ^ 3)) / (x_vect(n) - x_vect(n - 1))
        a = y_vect(n - 1) - b * x_vect(n - 1) - c * x_vect(n - 1) ^ 2 - d * x_vect(n - 1) ^ 3

        '1st ORDER DERIVATIVES OF INTERMEDIATE POINTS

        khjCubicSplineEngDouble = d * x_value ^ 3 + c * x_value ^ 2 + b * x_value + a
        ansVar(0, 2) = "Interval: " & xArrayBoundCount - n + 1 & " TO " & xArrayBoundCount - n + 2
        ansVar(0, 0) = khjCubicSplineEngDouble
        khjCubicSplineEngDetails = ansVar

        Application.Calculation = xlCalculationAutomatic ' TUR ON AUTOMATIC CALCULATION AGAIN

End Function

     */



