import org.example.PlantUmlGenerator
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class PlantUmlGeneratorTest {
    
    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            setIdeaIoUseFallback()
        }
    }
    
    private val project by lazy {
        KotlinCoreEnvironment.createForProduction(
            Disposer.newDisposable(),
            CompilerConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        ).project
    }
    private val sourceDirPath = ""
    
    @Test
    fun `test PlantUML generation for all workflows`() {
        val sourceDirectory = File(sourceDirPath)
        val workflowClasses = findWorkflowImplementations(sourceDirectory)
        
        check(workflowClasses.isNotEmpty()) { "No workflows found" }
        
        workflowClasses.forEach { (className, psiClass) ->
            val plantUmlCode = PlantUmlGenerator().generatePlantUmlFromPsi(psiClass)
            File("src/test/out/$className.puml").writeText(plantUmlCode)
            println("PlantUML for $className generated!")
        }
    }
    
    private fun findWorkflowImplementations(directory: File): Map<String, KtClass> {
        val workflowClasses = mutableMapOf<String, KtClass>()
        
        directory.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val fileContent = file.readText()
            val psiFile = PsiFileFactory.getInstance(project)
                .createFileFromText(file.name, KotlinLanguage.INSTANCE, fileContent) as KtFile
            
            psiFile.collectDescendantsOfType<KtClass>().forEach { ktClass ->
                if (ktClass.superTypeListEntries.any { it.text.contains("WorkflowInterface") }) {
                    workflowClasses[ktClass.name ?: "UnknownWorkflow"] = ktClass
                }
            }
        }
        return workflowClasses
    }
    
    @Test
    fun `generate PlantUML for specific class`() {
        val className = "BuildListWorkflow"
        val sourceDirectory = File(sourceDirPath)
        
        val psiClass = findClassByName(sourceDirectory, className)
        checkNotNull(psiClass) {"Class '$className' not found!" }
        
        val plantUmlCode = PlantUmlGenerator().generatePlantUmlFromPsi(psiClass)
        val outputFile = File("src/test/out/$className.puml")
        outputFile.writeText(plantUmlCode)
        
        println("PlantUML for class '$className' created: ${outputFile.absolutePath}")
    }
    
    private fun findClassByName(directory: File, className: String): KtClass? {
        directory.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val fileContent = file.readText()
            val psiFile = PsiFileFactory.getInstance(project)
                .createFileFromText(file.name, KotlinLanguage.INSTANCE, fileContent) as KtFile
            
            val foundClass = psiFile.collectDescendantsOfType<KtClass>().find { it.name == className }
            if (foundClass != null) return foundClass
        }
        return null
    }
}
