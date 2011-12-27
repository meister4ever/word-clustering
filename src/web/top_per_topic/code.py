import db
import re
import web
import view, config
from view import render

urls = (
    '/', 'index',
    '/show', 'show_category',
    '/show_words', 'show_category_words'
)

seedwords = {}

def fill_seedwords():
  seedwords_file = open('./thesaurus.latest.txt')
  for line in seedwords_file:
    line = line.strip()
    category, word_str = line.split(':')
    words = [w.strip() for w in word_str.split(',')]
    seedwords[category] = words
  seedwords_file.close()

class index:
    def GET(self):
        return render.base(view.listing())

class show_category:
    def GET(self):
        fill_seedwords()
        category = None
        category_list = {}
        user_data = web.input()
        if user_data.has_key('category'):
          category = user_data.get('category')
          k = {}
          k['what'] = 'rank, name'
          k['where'] = 'category = "%s"' % category
          entities = db.listing(**k)
          category_list['category'] = category
          category_list['entities'] = entities
        return render.base(view.listing(), category_list = category_list)

class show_category_words:
    def GET(self):
        fill_seedwords()
        category = None
        category_list = {}
        user_data = web.input()
        if user_data.has_key('category'):
          category = user_data.get('category')
          k = {}
          k['what'] = 'rank, name'
          k['where'] = 'category = "%s"' % category
          entities = db.listing(**k)
          category_list['category'] = category
          category_list['words'] = seedwords[category]
        return render.base(view.listing(), category_list = category_list)

if __name__ == "__main__":
    app = web.application(urls, globals())
    app.internalerror = web.debugerror
    app.run()
