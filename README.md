# Storyland
Storyland is an Android application that seeks to entertain the little ones in an innovative way. This application allows you to create your own stories and then play them with your friends.

Make decisions, look for solutions and experience unique stories. Thanks to voice customization, you can feel part of your own story!

This application is guaranteed hours of fun for children of all ages!

## Features
### Customize your voice
To make each story unique, we use the customization of the **TextToSpeech** service, which allows us to choose between different voices, change their tone or even their speed. This way we are able to create a voice adapted to each player.

### Interactive stories
Thanks to the **Accelerometer** sensor, the application is capable of providing a more immersive experience.

- Is a bee chasing you? Run away from her!
- Have you wet your hands? Shake them off!

The possibilities with this sensor are endless!

### Synchronization
**Bluetooth** is used to synchronize stories between different devices. Not only are the stories sent, but they run synchronously on two different devices.

This is achieved thanks to a **Thread** that is in charge of the logical part of the game.

### Decision making
Stories can take one path or another. You decide what happens in the story!

Now it is possible to store the different paths, being able to replay a story and obtain different endings.

### Create your own stories!
You can create your own story from scratch!

Here the **RecyclerView** is used to display the sequence of a story. In turn, this activity connects to the **SQLite** database through a **ContentProvider**, in order to add and modify the story data.
