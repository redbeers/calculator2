package com.stella.calculator2

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var processView: TextView
    private lateinit var resultView: TextView

    private var fullExpression = ""
    private var lastInputWasOperator = false
    private var decimalAdded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupButtonListeners()
    }

    private fun initializeViews() {
        processView = findViewById(R.id.process_view)
        resultView = findViewById(R.id.result_view)

        // 긴 텍스트 스크롤 처리
        processView.movementMethod = ScrollingMovementMethod()
        resultView.movementMethod = ScrollingMovementMethod()
    }

    private fun setupButtonListeners() {
        val buttonMap = mapOf(
            R.id.btn_0 to "0", R.id.btn_1 to "1",
            R.id.btn_2 to "2", R.id.btn_3 to "3",
            R.id.btn_4 to "4", R.id.btn_5 to "5",
            R.id.btn_6 to "6", R.id.btn_7 to "7",
            R.id.btn_8 to "8", R.id.btn_9 to "9",
            R.id.btn_00 to "00",
            R.id.btn_dot to ".",
            R.id.btn_plus to "+",
            R.id.btn_minus to "-",
            R.id.btn_multiply to "*",
            R.id.btn_divide to "/"
        )

        buttonMap.forEach { (buttonId, value) ->
            findViewById<Button>(buttonId).setOnClickListener {
                handleInput(value)
            }
        }

        findViewById<Button>(R.id.btn_equals).setOnClickListener { calculateResult() }
        findViewById<Button>(R.id.btn_clean).setOnClickListener { clearCalculator() }
        findViewById<Button>(R.id.btn_backspace).setOnClickListener { deleteLastChar() }
        findViewById<Button>(R.id.btn_percent).setOnClickListener { handlePercentage() }
    }

    private fun handleInput(input: String) {
        when {
            input in listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "00", ".") -> handleNumberInput(input)
            input in listOf("+", "-", "*", "/") -> handleOperatorInput(input)
        }
    }

    private fun handleNumberInput(number: String) {
        // "00" 입력 시 특별 처리
        if (number == "00") {
            // 첫 입력이거나 마지막 문자가 연산자인 경우 무시
            if (fullExpression.isEmpty() || fullExpression.last() in listOf('+', '-', '*', '/')) {
                return
            }
        }

        // 소수점 중복 방지
        if (number == "." && decimalAdded) return

        if (number == ".") decimalAdded = true
        fullExpression += number
        updateViews()
        lastInputWasOperator = false
    }

    private fun handleOperatorInput(operator: String) {
        // 연산자 연속 입력 방지
        if (lastInputWasOperator) return

        fullExpression += operator
        updateViews()
        lastInputWasOperator = true
        decimalAdded = false
    }

    private fun handlePercentage() {
        try {
            val result = evaluateExpression(fullExpression).divide(BigDecimal(100))
            fullExpression = formatResult(result)
            updateViews()
        } catch (e: Exception) {
            showErrorMessage("계산 오류")
        }
    }

    private fun calculateResult() {
        try {
            val result = evaluateExpression(fullExpression)
            fullExpression = formatResult(result)
            processView.text = fullExpression
            resultView.text = ""
            lastInputWasOperator = false
            decimalAdded = fullExpression.contains(".")
        } catch (e: Exception) {
            showErrorMessage("계산 오류")
        }
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        clearCalculator()
    }

    private fun deleteLastChar() {
        if (fullExpression.isNotEmpty()) {
            val lastChar = fullExpression.last()
            fullExpression = fullExpression.dropLast(1)

            // 삭제된 문자에 따라 상태 업데이트
            if (lastChar == '.') decimalAdded = false
            if (lastChar in listOf('+', '-', '*', '/')) lastInputWasOperator = false

            updateViews()
        }
    }

    private fun clearCalculator() {
        fullExpression = ""
        resultView.text = ""
        processView.text = ""
        lastInputWasOperator = false
        decimalAdded = false
    }

    private fun updateViews() {
        processView.text = fullExpression

        // 실시간 결과 계산
        try {
            val result = evaluateExpression(fullExpression)
            // 결과가 너무 큰 경우 과학적 표기법으로 변환
            resultView.text = if (result.abs().compareTo(BigDecimal(1e20)) > 0) {
                result.toString().replace("E", "×10^")
            } else {
                formatResult(result)
            }
        } catch (e: Exception) {
            resultView.text = ""
        }
    }

    private fun evaluateExpression(expression: String): BigDecimal {
        try {
            val regex = """([\+\-\*\/]|\d+(?:\.\d+)?)""".toRegex()
            val tokens = regex.findAll(expression).map { it.value }.toList()

            val values = Stack<BigDecimal>()
            val ops = Stack<String>()

            for (token in tokens) {
                when {
                    // BigDecimal로 변환
                    token.toBigDecimalOrNull() != null -> values.push(BigDecimal(token))
                    token == "(" -> ops.push(token)
                    token == ")" -> {
                        while (ops.peek() != "(") values.push(applyOp(ops.pop(), values.pop(), values.pop()))
                        ops.pop()
                    }
                    token in setOf("+", "-", "*", "/") -> {
                        while (ops.isNotEmpty() && hasPrecedence(token, ops.peek()))
                            values.push(applyOp(ops.pop(), values.pop(), values.pop()))
                        ops.push(token)
                    }
                }
            }

            while (ops.isNotEmpty()) values.push(applyOp(ops.pop(), values.pop(), values.pop()))

            return values.pop()
        } catch (e: Exception) {
            throw Exception("계산 오류")
        }
    }

    private fun hasPrecedence(op1: String, op2: String): Boolean {
        if (op2 == "(" || op2 == ")") return false
        if ((op1 == "*" || op1 == "/") && (op2 == "+" || op2 == "-")) return false
        return true
    }

    private fun applyOp(op: String, b: BigDecimal, a: BigDecimal): BigDecimal {
        return when (op) {
            "+" -> a.plus(b)
            "-" -> a.minus(b)
            "*" -> a.multiply(b)
            "/" -> try {
                // 소수점 10자리까지 반올림
                a.divide(b, 10, RoundingMode.HALF_UP)
            } catch (e: ArithmeticException) {
                throw UnsupportedOperationException("0으로 나눌 수 없습니다")
            }
            else -> BigDecimal.ZERO
        }
    }

    private fun formatResult(value: BigDecimal): String {
        return try {
            // 정수인 경우 정수로 표시
            if (value.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
                value.toBigInteger().toString()
            } else {
                // 소수점 10자리까지 표시
                value.setScale(10, RoundingMode.HALF_UP).toString().trimEnd('0').trimEnd('.')
            }
        } catch (e: Exception) {
            "오류"
        }
    }
}