//
//  VideoEditRequest.swift
//  broody_video
//
//  Created by Tim on 02.08.22.
//

import Foundation

class VideoEditRequest {

    static let sourcePathKey: String = "sourcePath"
    static let targetWidthKey: String = "targetWidth"
    static let targetHeight: String = "targetHeight"

    let sourcePath: String
    let targetSize: CGSize

    init(arguments: Dictionary<String, Any>) {
        sourcePath = arguments[VideoEditRequest.sourcePathKey] as! String;
        let width = arguments[VideoEditRequest.targetWidthKey] as! Int;
        let height = arguments[VideoEditRequest.targetWidthKey] as! Int;
        targetSize = CGSize(width: width, height: height)
    }
}

class VideoEditor {
    func cutClip(_ request: VideoEditRequest) {

    }
}
