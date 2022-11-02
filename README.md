# android-room-compound
A light-weight code generation library that helps reducing boilerplate code when working with relations in Room

## Definition
A **Compound** is a simple data class holding two or more entities, or even other compounds, and defining a relationship between them.

For example, we have a `User` model in our domain, that contains some other models such as `Avatar`, `Banner`... etc

The equivalent of User in our data layer will be `UserCompound`, containing an embedded `UserEntity`, and defining a relationship with `AvatarEntity` and `BannerEntity`.

> Compound is not a very common name for objects, but still better than the traditional naming used amoung android developers when working with relations in room, such as `UserWithAvatarAndBanner`. Extending our user will be so painful the more we add more relations.

## Installation
### build.gradle (project level)
This library is available on `mavenCentral`, so make sure to add this repository:
```kotlin
buildscript {
    repositories {
        mavenCentral()
    }
}
```

Also, we need to add `ksp` plugin, since it is used for annotation processing:
```kotlin
plugins {
    id("com.google.devtools.ksp") version "$ksp_version"
}
```
> Check build.gradle of this library for the compatible version of ksp [here](https://github.com/tamimattafi/android-room-compound/blob/main/build.gradle)

### build.gradle (module level)
In our gradle script of our library or application, we need to apply `ksp` plugin:
```kotlin
plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
}
```

Then we add the required dependencies:
```kotlin
dependencies {
    implementation("com.attafitamim.room:compound-annotations:$version")
    ksp("com.attafitamim.room:compound-processor:$version")
}
```
> Check release notes for the latest version at [here](https://github.com/tamimattafi/android-room-compound/releases)

## Usage
The usage is very simple, just create a `Compound` data class with the required entities and other compounds, then annotate it with `@Compound` annotation:
```kotlin
@Compound
data class MainCompound(
    @Embedded
    val mainEntity: MainEntity,

    @Relation(
        parentColumn = "name",
        entityColumn = "name",
        associateBy = Junction(
            value = MainSecondJunction::class,
            parentColumn = "mainId",
            entityColumn = "secondId"
        )
    )
    val secondaryCompounds: List<SecondCompound>?,

    @Relation(
        parentColumn = "name",
        entityColumn = "name",
        associateBy = Junction(
            value = MainSecondJunction::class,
            parentColumn = "mainId",
            entityColumn = "secondId"
        )
    )
    val secondaryEntities: List<SecondEntity>?,

    @Relation(
        parentColumn = "name",
        entityColumn = "name"
    )
    val secondaryCompound: SecondCompound?
)
```

After a successful build, a dao will be generated, we need to add it to our `ApplicationDatabase`:
```kotlin
@Database(
    entities = [
        MainEntity::class,
        MainSecondJunction::class,
        SecondEntity::class,
        SecondThirdJunction::class,
        ThirdEntity::class,
        ForthEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MainDatabase : RoomDatabase() {
    abstract val mainCompoundDao: IMainCompoundDao
    abstract val secondCompoundDao: ISecondCompoundDao
    abstract val thirdCompoundDao: IThirdCompoundDao
    abstract val mainCompound: MainCompound
}
```
> Note that since room allows us to implement other daos, we can actually have `IMainDao` implement `IMainCompoundDao` so we can add more methods, and still have our generated ones.

Using this dao would look like this:
```kotlin
        val secondEntity = SecondEntity("mainEntity", "1/2/3")
        val forthEntity = ForthEntity("forthEntity", "1/2/3/4")
        val thirdEntity = ThirdEntity("thirdEntity", "1/2/3")
        val thirdCompound = ThirdCompound(thirdEntity, listOf(secondEntity), listOf(forthEntity))

        val secondCompound = SecondCompound(
            secondEntity,
            listOf(thirdCompound),
            listOf(thirdEntity),
            thirdCompound,
            forthEntity
        )

        val mainEntity = MainEntity("mainEntity", "1/2/3")
        val mainCompound = MainCompound(mainEntity, listOf(secondCompound), listOf(secondEntity), secondCompound)
        database.mainCompoundDao.insertOrUpdate(mainCompound)
```

> Please check the sample app for more information [here](https://github.com/tamimattafi/android-room-compound/tree/main/sample)

## Settings
You can customize the generated code by defining these rules in your `build.gradle` (module level)
```kotlin
ksp {
    // Dao's methods will be suspended
    arg("suspendDao", "true")
    // Add "I" as a prefix to dao's interface
    arg("useDaoPrefix", "true")
    // Add "Dao" postfix to dao's interface
    arg("useDaoPostfix", "true")
}
```
> More settings will be supported in the future

## Licence
Apache License 2.0
A permissive license whose main conditions require preservation of copyright and license notices. Contributors provide an express grant of patent rights. Licensed works, modifications, and larger works may be distributed under different terms and without source code.

More information [here](https://github.com/tamimattafi/android-room-compound/blob/main/LICENSE)
