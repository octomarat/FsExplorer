# FsExplorer

Простой менеджер файловой системы. Позволяет просматривать содержимое локальной файловой системы (`/` для Unix и `C:\` для Windows), просматривать содержимое папок по FTP, просматривать содержимое вложенных архивов. Для всех этих случаев есть возможность посмотреть превью текстовых файлов и изображений.

# Общее описание архитектуры

Приложение состоит из нескольких модулей. Основной функционал и точка входа расположены в модуле App.

Приложение построено по принципу MVC. Пакет `fs.explorer.views` содержит классы, котрые представляют элементы графического интерфейса (`MenuBar`, `FTPDialog` и т.д.). События элементов GUI (выбор пунктов меню, разворачивание дерева каталогов) обрабатываются соответствующими контроллерами, которые лежат в пакете `fs.explorer.controllers`. Для получения данных о содержании каталогов и архивов, а так же для построения превью файлов, контроллеры пользуются провайдерами из пакета `fs.explorer.providers`. Получив нужную информацию, контроллеры обновляют модели из `fs.explorer.models` и элементы GUI.

# Сборка и запуск приложения

1. Собрать все модули. В IDEA: `Maven Projects > fs-explorer > Lifecycle > package`.
2. Собрать модуль App со всеми зависимостями. В IDEA: `Maven Projects > App > Plugins > assembly > assembly:single`.
3. Запустить App. В IDEA: `Maven Projects > App > Plugins > exec > exec:java`.

# Добавление новых видов превью

Для добавления нового типа превью необходимо:
1. Написать класс-отрисовщик, который реализует интерфейс `fs.explorer.providers.preview.PreviewRenderer` из модуля RenderersService.
2. Положить реализованный класс в модуль Renderers. 
3. Пересобрать jar-архив модуля Renderers.
Далее, можно пересобрать все приложение и новый отрисовщик превью автоматически будет использован программой. Однако, полная пересборка не является обязательной. Можно просто запустить приложение и передать ему на вход jar-файл модулья Renderers с добавленными/исправленными отрисовщиками. Пример запуска из командной строки:

`java -Djava.ext.dirs=${PROJECT_PATH}/Common/target:${PROJECT_PATH}/RenderersService/target:${PROJECT_PATH}/Renderers/target -jar ${PROJECT_PATH}/App/target/App-1.0-SNAPSHOT-jar-with-dependencies.jar`

Здесь мы передаем путь до jar-файла модуля Renderers, а так же пути до jar-файлов модулей Common и RenderersService, так как Renderers зависит от них.

Автоматическая загрузка отрисовщиков реализована через через `java.util.ServiceLoader`.

# Ограничения

1. Автоматическое обновление содержимого папок и архивов не поддерживается.
2. Ручное обновление содержимого папок поддерживается, а содержимого архивов -- нет.
3. При построении превью для картинок, приложение не умеет определять, что изображение испорчено или имеет неверный формат. Вместо испорченного изображения просто показывается пустое превью. Превью для изображений строится с помощью `javax.swing.ImageIcon`. Этот класс, судя по всему, предоставляет очень ограниченные возможности по определению испорченности формата изображения. Вероятно, для корректной обработки лучше использовать внешнюю библиотеку для загрузки изображений.
4. При построении превью текста используется стандартная кодировка, возможности выбрать другую кодировку нет (это довольно легко исправить).
5. При чтении содержимого архивов используется стандартная кодировка, возможности выбрать другую кодировку нет (тоже легко исправить). В результате, названия папок и файлов внутри архивов могут быть некорректными.
6. При чтении некорректных архивов приложение отображает их содержимое как пустое, а не сообщает об ошибке. Для чтения используется класс `java.util.zip.ZipInputStream`, который не умеет определять, что архив испорчен, он просто говорит, что в нем нет записей.

# Тесты

Помимо обычных тестов имеются тесты которые делают запросы по FTP к публичным серверам и тесты асинхронного кода. По умолчанию они выключены посредством механизма `Assume.assumeTrue` и условий в классе `fs.explorer.TestEnvironment`. Чтобы запустить эти тесты, нужно добавить к параметрам запуска системные свойства через `-D`. Названия свойств можно посмотреть в классе `TestEnvironment`.
