//
//  VRORenderDelegate.h
//  ViroRenderer
//
//  Created by Raj Advani on 12/15/15.
//  Copyright © 2015 Viro Media. All rights reserved.
//

#ifndef VRORenderDelegate_h
#define VRORenderDelegate_h

@class VROView;
class VRORenderContext;
class VROVector3f;

typedef NS_ENUM(NSInteger, VROEyeType) {
    VROEyeTypeMonocular,
    VROEyeTypeLeft,
    VROEyeTypeRight,
};

@protocol VRORenderDelegate <NSObject>

- (void)setupRendererWithView:(VROView *)view context:(VRORenderContext *)context;
- (void)shutdownRendererWithView:(VROView *)view;
- (void)renderViewDidChangeSize:(CGSize)size context:(VRORenderContext *)context;

- (void)willRenderEye:(VROEyeType)eye context:(const VRORenderContext *)context;
- (void)didRenderEye:(VROEyeType)eye context:(const VRORenderContext *)context;
- (void)reticleTapped:(VROVector3f)ray;

@end

#endif /* VRORenderDelegate_h */
