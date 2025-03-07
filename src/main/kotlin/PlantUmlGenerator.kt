package org.example

import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class PlantUmlGenerator {
    
    fun generatePlantUmlFromPsi(psiClass: KtClass): String {
        val sb = StringBuilder()
        sb.appendLine("@startuml")
        sb.appendLine("start")
        
        val method = psiClass.collectDescendantsOfType<KtNamedFunction>().find { it.name == "getActivityList" }
        checkNotNull(method) { "Method 'getActivityList' in ${psiClass.name} not found!" }
        
        val activityListName = findActivityListName(method)
        method.bodyExpression?.let { parseExpression(it, sb, activityListName) }
        
        sb.appendLine("stop")
        sb.appendLine("@enduml")
        return sb.toString()
    }
    
    fun findActivityListName(method: KtNamedFunction): String {
        val propertyDeclarations = method.bodyExpression?.collectDescendantsOfType<KtProperty>() ?: return "activities"
        return propertyDeclarations.find { it.initializer?.text?.contains("mutableListOf") == true }?.name ?: "activities"
    }
    
    fun parseExpression(expression: KtExpression, sb: StringBuilder, activityListName: String, indent: Int = 0) {
        when (expression) {
            is KtReturnExpression -> {
                expression.returnedExpression?.let { parseExpression(it, sb, activityListName, indent) }
            }
            is KtBlockExpression -> {
                expression.statements.forEach { parseExpression(it, sb, activityListName, indent) }
            }
            is KtDotQualifiedExpression -> {
                expression.selectorExpression?.let { parseExpression(it, sb, activityListName, indent) }
            }
            is KtCallExpression -> {
                val functionName = expression.calleeExpression?.text ?: "Unknown"
                if (functionName == "add" || functionName == "plusAssign") {
                    val firstArg = expression.valueArguments.firstOrNull()?.text ?: "Unknown"
                    sb.appendLine("${"    ".repeat(indent)}:$firstArg;")
                } else if (functionName == "buildList") {
                    expression.lambdaArguments.firstOrNull()?.getLambdaExpression()?.bodyExpression?.statements?.forEach {
                        parseExpression(it, sb, activityListName, indent)
                    }
                }
            }
            is KtBinaryExpression -> {
                val left = expression.left?.text ?: return
                val right = expression.right?.text ?: return
                if ((left == "this" || left == activityListName) && expression.operationReference.text == "+=") {
                    sb.appendLine("${"    ".repeat(indent)}:$right;")
                }
            }
            is KtIfExpression -> {
                val condition = expression.condition?.text ?: "Unknown Condition"
                sb.appendLine("${"    ".repeat(indent)}if ($condition?) then (yes)")
                
                expression.then?.let { parseExpression(it, sb, activityListName, indent + 1) }
                
                expression.`else`?.let {
                    sb.appendLine("${"    ".repeat(indent)}else (no)")
                    parseExpression(it, sb, activityListName, indent + 1)
                }
                sb.appendLine("${"    ".repeat(indent)}endif")
            }
            is KtWhenExpression -> {
                val subject = expression.subjectExpression?.text ?: "Unknown"
                sb.appendLine("${"    ".repeat(indent)}switch ($subject)")
                
                for (entry in expression.entries) {
                    val conditions = entry.conditions.joinToString(", ") { it.text }
                    sb.appendLine("${"    ".repeat(indent)}case (\"$conditions\")")
                    parseExpression(entry.expression ?: continue, sb, activityListName, indent + 1)
                }
                sb.appendLine("${"    ".repeat(indent)}endswitch")
            }
            is KtForExpression -> { // TODO currently broken.
                val loopVar = expression.loopParameter?.text ?: "Unknown"
                val loopRange = expression.loopRange?.text ?: "Unknown Range"
                sb.appendLine("${"    ".repeat(indent)}repeat ($loopRange as $loopVar)")
                parseExpression(expression.body ?: return, sb, activityListName, indent + 1)
                sb.appendLine("${"    ".repeat(indent)}endrepeat")
            }
            is KtWhileExpression -> {
                val condition = expression.condition?.text ?: "Unknown Condition"
                sb.appendLine("${"    ".repeat(indent)}while ($condition)")
                parseExpression(expression.body ?: return, sb, activityListName, indent + 1)
                sb.appendLine("${"    ".repeat(indent)}endwhile")
            }
        }
    }
}
