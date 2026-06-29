# Keep Route Optimization engine
-keep class com.smartcourier.app.domain.usecase.OptimizerEngine { *; }

# Keep Room entities
-keep class com.smartcourier.app.data.local.entity.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
