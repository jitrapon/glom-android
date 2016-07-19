# Glom (This name will be subject to change later)
This repository contains the project codebase for an ongoing development of the Android application. The app allows user for collaboration, event planning, instant messaging, and various AI-assisted tasks.

# Features
* Intelligent chatbots that can help answer questions while chatting
    * Implemented with [wit.ai](https://wit.ai/)
* Easily share everything on a single board, (events, tasks, notes, drawings, locations, files, and more)
* Easily search for new upcoming events, movies, things to do around town, with the power of AI (suggestion algorithm based on your preferences)
* Easily scan your emails and use machine-learning to classify important messages, meetings, and pin to said board (along with all other notes, events, etc.)
* Use machine learning to provide best suggestions for you, for example, if you ask the chatbot that you want to it to find good japanese restaurants on thursday every week

# Backend
[Server Repo](https://github.com/Yoshi3003/glom-server/)

# How to build and run
1. Clone this repo and open the project with Android Studio.
2. In build.gradle (app module), look for `SERVER_URL` under buildTypes and change it to a specific IP of the server.
