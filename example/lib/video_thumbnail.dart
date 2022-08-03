import 'dart:io';
import 'dart:typed_data';

import 'package:broody_video/broody_video.dart';
import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

class VideoThumbnail extends StatefulWidget {
  @override
  _VideoThumbnailState createState() => _VideoThumbnailState();
}

class _VideoThumbnailState extends State<VideoThumbnail> {
  Uint8List? _thumbnail;

  @override
  Widget build(BuildContext context) {
    Future<Null> _getVideoThumbnail() async {
      var file;

      if (Platform.isMacOS) {
        final typeGroup =
            XTypeGroup(label: 'videos', extensions: ['mov', 'mp4']);
        file = await openFile(acceptedTypeGroups: [typeGroup]);
      } else {
        final picker = ImagePicker();
        var pickedFile = await picker.pickVideo(source: ImageSource.gallery);
        file = File(pickedFile!.path);
      }

      if (file != null) {
        final thumb = await BroodyVideo.instance.getThumbnail(sourceFile: file);
        setState(() {
          _thumbnail = thumb;
        });
      } else {
        return null;
      }
    }

    return Scaffold(
      appBar: AppBar(title: Text('File Thumbnail')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Container(
                child: ElevatedButton(
                    onPressed: _getVideoThumbnail,
                    child: Text('Get File Thumbnail'))),
            _buildThumbnail(),
          ],
        ),
      ),
    );
  }

  Widget _buildThumbnail() {
    if (_thumbnail != null) {
      return Container(
        padding: EdgeInsets.all(20.0),
        child: Image(image: MemoryImage(_thumbnail!)),
      );
    }
    return Container();
  }
}
