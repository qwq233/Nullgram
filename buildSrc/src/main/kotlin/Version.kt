import org.gradle.api.JavaVersion

object Version {

    @JvmStatic
    val java = JavaVersion.VERSION_17

    @JvmStatic
    val officialVersionName = "10.0.1"

    @JvmStatic
    val isStable = true
}
