# GlyphCraft

## Fabric compatibility

The Fabric distribution targets Minecraft Java 1.21 through 1.21.11. The
loader range is declared in `fabric.mod.json`; build a jar against the exact
Minecraft/Fabric API patch used by the instance (for example, 1.21.10 or
1.21.11) so mappings and mixin signatures are checked by the compiler.

Контекстный клиентский редактор символов, книг и табличек для Minecraft 1.21–1.21.11 (Fabric, Java 21+).

Что уже работает:

- GlyphCraft включается только внутри загруженного мира;
- интерфейс автоматически появляется только при открытии `Книги и пера` или редактировании таблички;
- в меню, названии мира, чате, наковальне и других GUI мод ничего не рисует;
- книга/табличка остаётся по центру, действия вынесены в боковые панели, символы и цвета — в нижнюю ленту;
- более 900 символов из Unicode: рамки, стрелки, фигуры, математика, алфавиты, блоки, значки и эмодзи;
- вставка в позицию курсора и быстрый просмотр страницы;
- цвет текста, жирный/курсив, заголовок страницы;
- добавление и удаление страниц непосредственно в открытой ванильной книге;
- сохранение и загрузка JSON-книг/табличек в `.minecraft/config/glyphcraft/books`;
- безопасные имена файлов и совместимый формат, которым можно делиться.

## Сборка и запуск

```powershell
./gradlew.bat build
./gradlew.bat runClient
```

Готовый файл: `build/libs/glyphcraft-0.2.1.jar`.

## Сборка под конкретную версию

Для точной версии Minecraft используй автоматический скрипт — он сам подберёт
совместимую версию Fabric API и проверит mappings:

```powershell
.\build-fabric-version.ps1 1.21
.\build-fabric-version.ps1 1.21.11
```

Готовые проверенные JAR лежат в `dist/`. Один JAR не смешивает mappings разных
патчей: выбирай файл, соответствующий версии Minecraft.
