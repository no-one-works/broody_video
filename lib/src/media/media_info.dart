import 'dart:io';

class MediaInfo {
  final File file;
  final int width;
  final int height;

  final Duration duration;

  /// bytes
  final int filesize;

  final String? title;

  final String? author;

  /// [Android] API level 17
  final int? orientation;

  const MediaInfo({
    required this.file,
    required this.width,
    required this.height,
    required this.duration,
    required this.filesize,
    this.title,
    this.author,
    this.orientation,
  });

  MediaInfo.fromJson(Map<String, dynamic> map)
      : file = File(map["path"]),
        width = map["width"],
        height = map["height"],
        duration = Duration(
          milliseconds: (1000 * map["duration"]).toInt(),
        ),
        filesize = map["filesize"],
        title = map["title"],
        author = map["author"],
        orientation = map["orientation"];

  Map<String, dynamic> toJson() {
    final data = <String, dynamic>{};
    data["path"] = file.path;
    data["width"] = width;
    data["height"] = height;
    data['duration'] = duration;
    data['filesize'] = filesize;
    data['title'] = title;
    data['author'] = author;
    data['orientation'] = orientation;
    return data;
  }
}
